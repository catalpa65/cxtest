package com.example.ecommerce.modules.mq.model;

import java.time.LocalDateTime;

public record AsyncProcessLog(
        String eventId,
        AsyncEventType type,
        Long orderId,
        String transactionId,
        int attempts,
        AsyncProcessStatus status,
        String message,
        LocalDateTime at
) {
}
