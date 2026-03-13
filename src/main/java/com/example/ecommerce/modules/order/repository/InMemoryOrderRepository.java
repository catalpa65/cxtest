package com.example.ecommerce.modules.order.repository;

import com.example.ecommerce.modules.order.model.Order;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
@Profile("memory")
public class InMemoryOrderRepository implements OrderRepository {

    private final ConcurrentHashMap<Long, Order> orders = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(0);

    @Override
    public synchronized Order save(Order order) {
        LocalDateTime now = LocalDateTime.now();
        if (order.getId() == null) {
            order.setId(idGenerator.incrementAndGet());
            order.setCreatedAt(now);
        }
        order.setUpdatedAt(now);
        orders.put(order.getId(), order);
        return order;
    }

    @Override
    public Optional<Order> findById(Long id) {
        return Optional.ofNullable(orders.get(id));
    }

    @Override
    public List<Order> findByUserId(Long userId) {
        List<Order> result = new ArrayList<>();
        for (Order order : orders.values()) {
            if (userId.equals(order.getUserId())) {
                result.add(order);
            }
        }
        return result;
    }
}
