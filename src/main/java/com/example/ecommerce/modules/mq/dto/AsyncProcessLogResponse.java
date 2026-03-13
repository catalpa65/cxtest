package com.example.ecommerce.modules.mq.dto;

import com.example.ecommerce.modules.mq.model.AsyncEventType;
import com.example.ecommerce.modules.mq.model.AsyncProcessLog;
import com.example.ecommerce.modules.mq.model.AsyncProcessStatus;

import java.time.LocalDateTime;

public record AsyncProcessLogResponse(
        String eventId,
        AsyncEventType type,
        Long orderId,
        String transactionId,
        int attempts,
        AsyncProcessStatus status,
        String message,
        LocalDateTime at
) {

    public static AsyncProcessLogResponse from(AsyncProcessLog log) {
        return new AsyncProcessLogResponse(
                log.eventId(),
                log.type(),
                log.orderId(),
                log.transactionId(),
                log.attempts(),
                log.status(),
                log.message(),
                log.at()
        );
    }
}
