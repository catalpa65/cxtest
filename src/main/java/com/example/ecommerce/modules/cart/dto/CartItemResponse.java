package com.example.ecommerce.modules.cart.dto;

import com.example.ecommerce.modules.product.model.ProductStatus;

import java.math.BigDecimal;

public record CartItemResponse(
        Long productId,
        String productName,
        BigDecimal unitPrice,
        Integer quantity,
        BigDecimal lineAmount,
        ProductStatus productStatus
) {
}
