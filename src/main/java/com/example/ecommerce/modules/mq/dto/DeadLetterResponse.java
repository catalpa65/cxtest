package com.example.ecommerce.modules.mq.dto;

import com.example.ecommerce.modules.mq.model.AsyncEventType;
import com.example.ecommerce.modules.mq.model.DeadLetterMessage;

import java.time.LocalDateTime;

public record DeadLetterResponse(
        String eventId,
        AsyncEventType type,
        Long orderId,
        String transactionId,
        int attempts,
        String lastError,
        LocalDateTime failedAt
) {

    public static DeadLetterResponse from(DeadLetterMessage message) {
        return new DeadLetterResponse(
                message.eventId(),
                message.type(),
                message.orderId(),
                message.transactionId(),
                message.attempts(),
                message.lastError(),
                message.failedAt()
        );
    }
}
