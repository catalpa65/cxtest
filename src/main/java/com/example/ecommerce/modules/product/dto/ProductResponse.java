package com.example.ecommerce.modules.product.dto;

import com.example.ecommerce.modules.product.model.Product;
import com.example.ecommerce.modules.product.model.ProductStatus;

import java.math.BigDecimal;

public record ProductResponse(Long id, String name, String description, BigDecimal price, Integer stock, ProductStatus status) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                product.getStatus()
        );
    }
}
