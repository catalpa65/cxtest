package com.example.ecommerce.modules.product.repository;

import com.example.ecommerce.modules.product.model.Product;
import com.example.ecommerce.modules.product.model.ProductStatus;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
@Profile("memory")
public class InMemoryProductRepository implements ProductRepository {

    private final ConcurrentHashMap<Long, Product> storage = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(0);

    public InMemoryProductRepository() {
        seed("iPhone 16", "Apple 手机", new BigDecimal("6999.00"), 100);
        seed("Sony WH-1000XM6", "降噪耳机", new BigDecimal("2899.00"), 100);
    }

    @Override
    public Product save(Product product) {
        if (product.getId() == null) {
            product.setId(idGenerator.incrementAndGet());
            product.setCreatedAt(LocalDateTime.now());
        }
        product.setUpdatedAt(LocalDateTime.now());
        storage.put(product.getId(), product);
        return product;
    }

    @Override
    public Optional<Product> findById(Long id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<Product> findAll() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public synchronized boolean reserveStock(Long productId, int quantity) {
        Product product = storage.get(productId);
        if (product == null) {
            return false;
        }
        if (product.getStock() == null || product.getStock() < quantity) {
            return false;
        }
        product.setStock(product.getStock() - quantity);
        product.setUpdatedAt(LocalDateTime.now());
        return true;
    }

    @Override
    public synchronized void releaseStock(Long productId, int quantity) {
        Product product = storage.get(productId);
        if (product == null) {
            return;
        }
        int current = product.getStock() == null ? 0 : product.getStock();
        product.setStock(current + quantity);
        product.setUpdatedAt(LocalDateTime.now());
    }

    private void seed(String name, String description, BigDecimal price, int stock) {
        Product product = new Product();
        product.setName(name);
        product.setDescription(description);
        product.setPrice(price);
        product.setStock(stock);
        product.setStatus(ProductStatus.ON_SHELF);
        save(product);
    }
}
