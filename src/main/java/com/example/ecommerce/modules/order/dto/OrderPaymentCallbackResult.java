package com.example.ecommerce.modules.order.dto;

import com.example.ecommerce.modules.order.model.OrderStatus;

public record OrderPaymentCallbackResult(
        Long orderId,
        OrderStatus status,
        String transactionId,
        boolean idempotent
) {
}
