package com.example.ecommerce.modules.cart.repository;

import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;

@Repository
@Profile("prod")
public class RedisCartRepository implements CartRepository {

    private static final String CART_KEY_PREFIX = "cart:user:";
    private final StringRedisTemplate redisTemplate;

    public RedisCartRepository(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void addQuantity(Long userId, Long productId, int delta) {
        redisTemplate.opsForHash().increment(key(userId), productId.toString(), delta);
    }

    @Override
    public void setQuantity(Long userId, Long productId, int quantity) {
        redisTemplate.opsForHash().put(key(userId), productId.toString(), Integer.toString(quantity));
    }

    @Override
    public void removeItem(Long userId, Long productId) {
        redisTemplate.opsForHash().delete(key(userId), productId.toString());
    }

    @Override
    public Map<Long, Integer> getItems(Long userId) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key(userId));
        if (entries.isEmpty()) {
            return Map.of();
        }
        Map<Long, Integer> result = new HashMap<>(entries.size());
        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            Long productId = parseLong(entry.getKey());
            Integer quantity = parseInteger(entry.getValue());
            if (productId == null || quantity == null || quantity <= 0) {
                continue;
            }
            result.put(productId, quantity);
        }
        return result;
    }

    @Override
    public void clear(Long userId) {
        redisTemplate.delete(key(userId));
    }

    private String key(Long userId) {
        return CART_KEY_PREFIX + userId;
    }

    private Long parseLong(Object raw) {
        if (raw == null) {
            return null;
        }
        try {
            return Long.parseLong(raw.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Integer parseInteger(Object raw) {
        if (raw == null) {
            return null;
        }
        try {
            return Integer.parseInt(raw.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
