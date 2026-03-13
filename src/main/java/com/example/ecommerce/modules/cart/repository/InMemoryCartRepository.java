package com.example.ecommerce.modules.cart.repository;

import org.springframework.stereotype.Repository;
import org.springframework.context.annotation.Profile;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@Profile("memory")
public class InMemoryCartRepository implements CartRepository {

    private final ConcurrentHashMap<Long, ConcurrentHashMap<Long, Integer>> cartsByUser = new ConcurrentHashMap<>();

    @Override
    public void addQuantity(Long userId, Long productId, int delta) {
        cartsByUser
                .computeIfAbsent(userId, key -> new ConcurrentHashMap<>())
                .merge(productId, delta, Integer::sum);
    }

    @Override
    public void setQuantity(Long userId, Long productId, int quantity) {
        cartsByUser
                .computeIfAbsent(userId, key -> new ConcurrentHashMap<>())
                .put(productId, quantity);
    }

    @Override
    public void removeItem(Long userId, Long productId) {
        ConcurrentHashMap<Long, Integer> userCart = cartsByUser.get(userId);
        if (userCart == null) {
            return;
        }
        userCart.remove(productId);
        if (userCart.isEmpty()) {
            cartsByUser.remove(userId);
        }
    }

    @Override
    public Map<Long, Integer> getItems(Long userId) {
        Map<Long, Integer> userCart = cartsByUser.get(userId);
        if (userCart == null) {
            return Map.of();
        }
        return new HashMap<>(userCart);
    }

    @Override
    public void clear(Long userId) {
        cartsByUser.remove(userId);
    }
}
