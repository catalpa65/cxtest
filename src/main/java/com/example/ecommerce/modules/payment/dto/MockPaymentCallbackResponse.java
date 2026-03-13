package com.example.ecommerce.modules.payment.dto;

import com.example.ecommerce.modules.order.model.OrderStatus;

public record MockPaymentCallbackResponse(
        Long orderId,
        OrderStatus status,
        String transactionId,
        boolean idempotent
) {
}
