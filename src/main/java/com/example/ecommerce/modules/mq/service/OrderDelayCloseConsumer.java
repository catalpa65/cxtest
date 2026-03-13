package com.example.ecommerce.modules.mq.service;

import com.example.ecommerce.modules.mq.model.AsyncEventMessage;
import com.example.ecommerce.modules.mq.model.AsyncEventType;
import com.example.ecommerce.modules.order.model.Order;
import com.example.ecommerce.modules.order.model.OrderItem;
import com.example.ecommerce.modules.order.model.OrderStatus;
import com.example.ecommerce.modules.order.repository.OrderRepository;
import com.example.ecommerce.modules.product.repository.ProductRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class OrderDelayCloseConsumer implements AsyncEventConsumer {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public OrderDelayCloseConsumer(OrderRepository orderRepository, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    @Override
    public AsyncEventType supportType() {
        return AsyncEventType.ORDER_DELAY_CLOSE;
    }

    @Override
    public void consume(AsyncEventMessage event) {
        Order order = orderRepository.findById(event.getOrderId()).orElse(null);
        if (order == null) {
            return;
        }
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            return;
        }
        if (order.getExpiresAt() != null && !LocalDateTime.now().isAfter(order.getExpiresAt())) {
            throw new IllegalStateException("订单尚未到期: " + order.getId());
        }

        for (OrderItem item : order.getItems()) {
            productRepository.releaseStock(item.getProductId(), item.getQuantity());
        }
        order.setStatus(OrderStatus.TIMEOUT_CANCELED);
        orderRepository.save(order);
    }
}
