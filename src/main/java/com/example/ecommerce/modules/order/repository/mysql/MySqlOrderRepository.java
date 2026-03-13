package com.example.ecommerce.modules.order.repository.mysql;

import com.example.ecommerce.modules.order.model.Order;
import com.example.ecommerce.modules.order.model.OrderItem;
import com.example.ecommerce.modules.order.repository.OrderRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@Profile("prod")
public class MySqlOrderRepository implements OrderRepository {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;

    public MySqlOrderRepository(OrderMapper orderMapper, OrderItemMapper orderItemMapper) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
    }

    @Override
    @Transactional
    public Order save(Order order) {
        LocalDateTime now = LocalDateTime.now();
        if (order.getId() == null) {
            order.setCreatedAt(now);
            order.setUpdatedAt(now);
            orderMapper.insert(order);
        } else {
            order.setUpdatedAt(now);
            orderMapper.update(order);
            orderItemMapper.deleteByOrderId(order.getId());
        }
        persistOrderItems(order.getId(), order.getItems());
        return order;
    }

    @Override
    public Optional<Order> findById(Long id) {
        Order order = orderMapper.findById(id);
        if (order == null) {
            return Optional.empty();
        }
        order.setItems(orderItemMapper.findByOrderId(order.getId()));
        return Optional.of(order);
    }

    @Override
    public List<Order> findByUserId(Long userId) {
        List<Order> orders = orderMapper.findByUserId(userId);
        for (Order order : orders) {
            order.setItems(orderItemMapper.findByOrderId(order.getId()));
        }
        return orders;
    }

    private void persistOrderItems(Long orderId, List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        for (OrderItem item : items) {
            orderItemMapper.insert(orderId, item);
        }
    }
}
