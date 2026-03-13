package com.example.ecommerce.modules.order.dto;

import com.example.ecommerce.modules.order.model.Order;
import com.example.ecommerce.modules.order.model.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        OrderStatus status,
        List<OrderItemResponse> items,
        BigDecimal totalAmount,
        LocalDateTime expiresAt,
        String paymentTransactionId,
        LocalDateTime paidAt,
        LocalDateTime completedAt,
        LocalDateTime createdAt
) {

    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getStatus(),
                order.getItems().stream().map(OrderItemResponse::from).toList(),
                order.getTotalAmount(),
                order.getExpiresAt(),
                order.getPaymentTransactionId(),
                order.getPaidAt(),
                order.getCompletedAt(),
                order.getCreatedAt()
        );
    }
}
