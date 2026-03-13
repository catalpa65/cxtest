package com.example.ecommerce.modules.product.controller;

import com.example.ecommerce.common.api.ApiResponse;
import com.example.ecommerce.modules.product.dto.CreateProductRequest;
import com.example.ecommerce.modules.product.dto.ProductResponse;
import com.example.ecommerce.modules.product.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ApiResponse<ProductResponse> create(@Valid @RequestBody CreateProductRequest request) {
        return ApiResponse.ok(productService.create(request));
    }

    @GetMapping
    public ApiResponse<List<ProductResponse>> list() {
        return ApiResponse.ok(productService.list());
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(productService.detail(id));
    }

    @PutMapping("/{id}/on-shelf")
    public ApiResponse<ProductResponse> onShelf(@PathVariable Long id) {
        return ApiResponse.ok(productService.putOnShelf(id));
    }

    @PutMapping("/{id}/off-shelf")
    public ApiResponse<ProductResponse> offShelf(@PathVariable Long id) {
        return ApiResponse.ok(productService.pullOffShelf(id));
    }
}
