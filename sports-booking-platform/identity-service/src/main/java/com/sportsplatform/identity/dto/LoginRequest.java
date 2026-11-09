package com.sportsplatform.identity.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Vui lòng nhập số điện thoại hoặc email")
        String phoneOrEmail,

        @NotBlank(message = "Vui lòng nhập mật khẩu")
        String password
) {}