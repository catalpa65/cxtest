package com.example.ecommerce.modules.product.repository;

import com.example.ecommerce.modules.product.model.Product;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {

    Product save(Product product);

    Optional<Product> findById(Long id);

    List<Product> findAll();

    boolean reserveStock(Long productId, int quantity);

    void releaseStock(Long productId, int quantity);
}
