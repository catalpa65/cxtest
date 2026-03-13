package com.example.ecommerce.modules.mq.model;

import java.time.LocalDateTime;

public record DeadLetterMessage(
        String eventId,
        AsyncEventType type,
        Long orderId,
        String transactionId,
        int attempts,
        String lastError,
        LocalDateTime failedAt
) {
}
