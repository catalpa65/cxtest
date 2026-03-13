package com.example.ecommerce.modules.order.repository;

import com.example.ecommerce.modules.order.model.Order;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findById(Long id);

    List<Order> findByUserId(Long userId);
}
