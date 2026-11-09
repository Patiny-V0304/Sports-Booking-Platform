package com.sportsplatform.matchmaking.messaging;

import com.sportsplatform.common.dto.BookingEventPayload;
import com.sportsplatform.matchmaking.repository.TeamLobbyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class BookingEventConsumer {

    private final TeamLobbyRepository teamLobbyRepository;

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * Lắng nghe bất đồng bộ từ RabbitMQ:
     * Khi đơn đặt sân bên Booking Service bị HỦY -> Tự động chuyển phòng ghép kèo sang CANCELLED
     */
    @Transactional
    @RabbitListener(queues = "matchmaking.booking.events.queue")
    public void handleBookingEvent(BookingEventPayload event) {
        log.info("<== [RabbitMQ Consumer - Matchmaking] Nhận sự kiện {}: {}", event.eventType(), event.message());

        if ("BOOKING_CANCELLED".equals(event.eventType())) {
            teamLobbyRepository.findByBookingId(event.bookingId()).ifPresent(lobby -> {
                lobby.setStatus("CANCELLED");
                teamLobbyRepository.save(lobby);
                log.info("<== [Auto-Action] Đã tự động đóng phòng ghép kèo ID={} do đơn đặt sân {} bị hủy!",
                        lobby.getId(), event.bookingCode());
            });
        }
    }
}