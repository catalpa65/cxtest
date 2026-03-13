package com.example.ecommerce.modules.mq.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.Map;

@Configuration
@EnableRabbit
@Profile("prod")
public class RabbitMqConfig {

    public static final String MAIN_EXCHANGE = "ecommerce.event.exchange";
    public static final String MAIN_QUEUE = "ecommerce.event.queue";
    public static final String MAIN_ROUTING_KEY = "ecommerce.event";

    public static final String DELAY_EXCHANGE = "ecommerce.event.delay.exchange";
    public static final String DELAY_QUEUE = "ecommerce.event.delay.queue";
    public static final String DELAY_ROUTING_KEY = "ecommerce.event.delay";

    public static final String DEAD_EXCHANGE = "ecommerce.event.dead.exchange";
    public static final String DEAD_QUEUE = "ecommerce.event.dead.queue";
    public static final String DEAD_ROUTING_KEY = "ecommerce.event.dead";

    @Bean
    DirectExchange mainExchange() {
        return new DirectExchange(MAIN_EXCHANGE, true, false);
    }

    @Bean
    DirectExchange delayExchange() {
        return new DirectExchange(DELAY_EXCHANGE, true, false);
    }

    @Bean
    DirectExchange deadExchange() {
        return new DirectExchange(DEAD_EXCHANGE, true, false);
    }

    @Bean
    Queue mainQueue() {
        return new Queue(MAIN_QUEUE, true);
    }

    @Bean
    Queue delayQueue() {
        return new Queue(
                DELAY_QUEUE,
                true,
                false,
                false,
                Map.of(
                        "x-dead-letter-exchange", MAIN_EXCHANGE,
                        "x-dead-letter-routing-key", MAIN_ROUTING_KEY
                )
        );
    }

    @Bean
    Queue deadQueue() {
        return new Queue(DEAD_QUEUE, true);
    }

    @Bean
    Binding mainBinding(
            @Qualifier("mainQueue") Queue mainQueue,
            @Qualifier("mainExchange") DirectExchange mainExchange
    ) {
        return BindingBuilder.bind(mainQueue).to(mainExchange).with(MAIN_ROUTING_KEY);
    }

    @Bean
    Binding delayBinding(
            @Qualifier("delayQueue") Queue delayQueue,
            @Qualifier("delayExchange") DirectExchange delayExchange
    ) {
        return BindingBuilder.bind(delayQueue).to(delayExchange).with(DELAY_ROUTING_KEY);
    }

    @Bean
    Binding deadBinding(
            @Qualifier("deadQueue") Queue deadQueue,
            @Qualifier("deadExchange") DirectExchange deadExchange
    ) {
        return BindingBuilder.bind(deadQueue).to(deadExchange).with(DEAD_ROUTING_KEY);
    }

}
