package com.example.ecommerce.modules.product.repository.mysql;

import com.example.ecommerce.modules.product.model.Product;
import com.example.ecommerce.modules.product.repository.ProductRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@Profile("prod")
public class MySqlProductRepository implements ProductRepository {

    private final ProductMapper productMapper;

    public MySqlProductRepository(ProductMapper productMapper) {
        this.productMapper = productMapper;
    }

    @Override
    public Product save(Product product) {
        LocalDateTime now = LocalDateTime.now();
        if (product.getId() == null) {
            product.setCreatedAt(now);
            product.setUpdatedAt(now);
            productMapper.insert(product);
            return product;
        }
        product.setUpdatedAt(now);
        productMapper.update(product);
        return product;
    }

    @Override
    public Optional<Product> findById(Long id) {
        return Optional.ofNullable(productMapper.findById(id));
    }

    @Override
    public List<Product> findAll() {
        return productMapper.findAll();
    }

    @Override
    public boolean reserveStock(Long productId, int quantity) {
        return productMapper.reserveStock(productId, quantity) > 0;
    }

    @Override
    public void releaseStock(Long productId, int quantity) {
        productMapper.releaseStock(productId, quantity);
    }
}
