package com.example.ecommerce.modules.payment.service;

import com.example.ecommerce.modules.order.dto.OrderPaymentCallbackResult;
import com.example.ecommerce.modules.order.service.OrderService;
import com.example.ecommerce.modules.payment.dto.MockPaymentCallbackRequest;
import com.example.ecommerce.modules.payment.dto.MockPaymentCallbackResponse;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    private final OrderService orderService;

    public PaymentService(OrderService orderService) {
        this.orderService = orderService;
    }

    public MockPaymentCallbackResponse mockCallback(MockPaymentCallbackRequest request) {
        String transactionId = request.getTransactionId().trim();
        OrderPaymentCallbackResult result = orderService.handlePaymentCallback(
                request.getOrderId(),
                transactionId,
                request.getPaidAmount()
        );
        return new MockPaymentCallbackResponse(
                result.orderId(),
                result.status(),
                result.transactionId(),
                result.idempotent()
        );
    }
}
