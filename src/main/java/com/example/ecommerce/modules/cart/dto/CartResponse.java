package com.example.ecommerce.modules.cart.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(List<CartItemResponse> items, Integer totalQuantity, BigDecimal totalAmount) {

    public static CartResponse empty() {
        return new CartResponse(List.of(), 0, BigDecimal.ZERO);
    }
}
