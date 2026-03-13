package com.example.ecommerce.modules.cart.controller;

import com.example.ecommerce.common.api.ApiResponse;
import com.example.ecommerce.common.auth.LoginRequired;
import com.example.ecommerce.modules.cart.dto.AddCartItemRequest;
import com.example.ecommerce.modules.cart.dto.CartResponse;
import com.example.ecommerce.modules.cart.dto.UpdateCartItemRequest;
import com.example.ecommerce.modules.cart.service.CartService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@LoginRequired
@RestController
@RequestMapping("/api/v1/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping("/items")
    public ApiResponse<CartResponse> addItem(@Valid @RequestBody AddCartItemRequest request) {
        return ApiResponse.ok(cartService.addItem(request));
    }

    @PutMapping("/items/{productId}")
    public ApiResponse<CartResponse> updateItem(
            @PathVariable Long productId,
            @Valid @RequestBody UpdateCartItemRequest request
    ) {
        return ApiResponse.ok(cartService.updateItem(productId, request));
    }

    @DeleteMapping("/items/{productId}")
    public ApiResponse<CartResponse> removeItem(@PathVariable Long productId) {
        return ApiResponse.ok(cartService.removeItem(productId));
    }

    @GetMapping
    public ApiResponse<CartResponse> detail() {
        return ApiResponse.ok(cartService.detail());
    }
}
