package com.example.ecommerce.modules.order.controller;

import com.example.ecommerce.common.api.ApiResponse;
import com.example.ecommerce.common.auth.LoginRequired;
import com.example.ecommerce.modules.order.dto.OrderResponse;
import com.example.ecommerce.modules.order.service.OrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@LoginRequired
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ApiResponse<OrderResponse> createOrder() {
        return ApiResponse.ok(orderService.createOrder());
    }

    @PostMapping("/{orderId}/cancel")
    public ApiResponse<OrderResponse> cancel(@PathVariable Long orderId) {
        return ApiResponse.ok(orderService.cancelOrder(orderId));
    }

    @PostMapping("/{orderId}/complete")
    public ApiResponse<OrderResponse> complete(@PathVariable Long orderId) {
        return ApiResponse.ok(orderService.completeOrder(orderId));
    }

    @GetMapping("/{orderId}")
    public ApiResponse<OrderResponse> detail(@PathVariable Long orderId) {
        return ApiResponse.ok(orderService.detail(orderId));
    }

    @GetMapping
    public ApiResponse<List<OrderResponse>> myOrders() {
        return ApiResponse.ok(orderService.myOrders());
    }
}
