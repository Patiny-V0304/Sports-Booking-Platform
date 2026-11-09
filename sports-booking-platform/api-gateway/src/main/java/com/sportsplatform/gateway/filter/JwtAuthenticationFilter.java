package com.sportsplatform.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        HttpMethod method = request.getMethod();

        // 1. Cho phép đi qua tự do đối với các API Public (Không cần đăng nhập)
        if (isPublicEndpoint(path, method)) {
            return chain.filter(exchange);
        }

        // 2. Kiểm tra Header Authorization: Bearer <token>
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return onError(exchange, "Thiếu JWT Token xác thực (Authorization: Bearer <token>)", HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.substring(7);
        try {
            // 3. Giải mã và xác thực chữ ký JWT
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String userId = claims.getSubject();
            String role = claims.get("role", String.class);

            // 4. Kiểm tra phân quyền nhanh cho các đường dẫn của Admin
            if (path.startsWith("/api/v1/admin/") && !"ADMIN".equals(role)) {
                return onError(exchange, "Bạn không có quyền Quản trị viên (ADMIN) để truy cập", HttpStatus.FORBIDDEN);
            }

            // 5. Đính kèm X-User-Id và X-User-Role vào Header truyền xuống các Microservices bên dưới
            ServerHttpRequest mutatedRequest = request.mutate()
                    .headers(httpHeaders -> {
                        httpHeaders.remove("X-User-Id");
                        httpHeaders.remove("X-User-Role");
                        httpHeaders.add("X-User-Id", userId);
                        httpHeaders.add("X-User-Role", role);
                    })
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (Exception e) {
            return onError(exchange, "JWT Token không hợp lệ hoặc đã hết hạn", HttpStatus.UNAUTHORIZED);
        }
    }

    private boolean isPublicEndpoint(String path, HttpMethod method) {
        // Cho phép mọi Preflight Request (OPTIONS) của trình duyệt đi qua
        if (HttpMethod.OPTIONS.equals(method)) {
            return true;
        }
        if (path.startsWith("/api/v1/auth/") || path.startsWith("/api/v1/payments/webhook")) {
            return true;
        }
        // Khách (Guest) được phép xem danh sách sân, lịch trống và danh sách phòng ghép kèo
        if (HttpMethod.GET.equals(method)) {
            return path.startsWith("/api/v1/facilities") || path.startsWith("/api/v1/lobbies");
        }
        return false;
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = """
                {"status": %d, "message": "%s"}
                """.formatted(status.value(), message);
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -1; // Ưu tiên chạy trước mọi bộ lọc khác
    }
}