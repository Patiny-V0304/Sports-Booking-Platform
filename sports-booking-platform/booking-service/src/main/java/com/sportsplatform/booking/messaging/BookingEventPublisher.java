package com.sportsplatform.booking.messaging;

import com.sportsplatform.common.dto.BookingEventPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishBookingEvent(String routingKeySuffix, BookingEventPayload payload) {
        String routingKey = "booking.event." + routingKeySuffix;
        log.info("==> [RabbitMQ Producer] Bắn sự kiện {}: {}", routingKey, payload);
        rabbitTemplate.convertAndSend(RabbitMqConfig.BOOKING_EXCHANGE, routingKey, payload);
    }
}