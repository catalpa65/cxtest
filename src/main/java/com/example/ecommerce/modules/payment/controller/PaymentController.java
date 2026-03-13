package com.example.ecommerce.modules.payment.controller;

import com.example.ecommerce.common.api.ApiResponse;
import com.example.ecommerce.modules.payment.dto.MockPaymentCallbackRequest;
import com.example.ecommerce.modules.payment.dto.MockPaymentCallbackResponse;
import com.example.ecommerce.modules.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/mock-callback")
    public ApiResponse<MockPaymentCallbackResponse> mockCallback(@Valid @RequestBody MockPaymentCallbackRequest request) {
        return ApiResponse.ok(paymentService.mockCallback(request));
    }
}
