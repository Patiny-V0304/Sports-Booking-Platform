package com.sportsplatform.booking.messaging;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String BOOKING_EXCHANGE = "booking.events.exchange";
    public static final String MATCHMAKING_QUEUE = "matchmaking.booking.events.queue";
    public static final String ROUTING_KEY = "booking.event.#";

    @Bean
    public TopicExchange bookingExchange() {
        return new TopicExchange(BOOKING_EXCHANGE);
    }

    @Bean
    public Queue matchmakingQueue() {
        return QueueBuilder.durable(MATCHMAKING_QUEUE).build();
    }

    @Bean
    public Binding binding(Queue matchmakingQueue, TopicExchange bookingExchange) {
        return BindingBuilder.bind(matchmakingQueue).to(bookingExchange).with(ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}