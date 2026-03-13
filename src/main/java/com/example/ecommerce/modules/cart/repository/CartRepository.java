package com.example.ecommerce.modules.cart.repository;

import java.util.Map;

public interface CartRepository {

    void addQuantity(Long userId, Long productId, int delta);

    void setQuantity(Long userId, Long productId, int quantity);

    void removeItem(Long userId, Long productId);

    Map<Long, Integer> getItems(Long userId);

    void clear(Long userId);
}
