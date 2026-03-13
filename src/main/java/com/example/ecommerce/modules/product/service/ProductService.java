package com.example.ecommerce.modules.product.service;

import com.example.ecommerce.common.error.ErrorCode;
import com.example.ecommerce.common.exception.BizException;
import com.example.ecommerce.modules.product.dto.CreateProductRequest;
import com.example.ecommerce.modules.product.dto.ProductResponse;
import com.example.ecommerce.modules.product.model.Product;
import com.example.ecommerce.modules.product.model.ProductStatus;
import com.example.ecommerce.modules.product.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class ProductService {

    private static final int DEFAULT_STOCK = 100;
    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public ProductResponse create(CreateProductRequest request) {
        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock() == null ? DEFAULT_STOCK : request.getStock());
        product.setStatus(ProductStatus.DRAFT);

        return ProductResponse.from(productRepository.save(product));
    }

    public List<ProductResponse> list() {
        return productRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(Product::getId))
                .map(ProductResponse::from)
                .toList();
    }

    public ProductResponse detail(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "商品不存在: " + id));
        return ProductResponse.from(product);
    }

    public ProductResponse putOnShelf(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "商品不存在: " + id));
        product.setStatus(ProductStatus.ON_SHELF);
        return ProductResponse.from(productRepository.save(product));
    }

    public ProductResponse pullOffShelf(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "商品不存在: " + id));
        product.setStatus(ProductStatus.OFF_SHELF);
        return ProductResponse.from(productRepository.save(product));
    }
}
