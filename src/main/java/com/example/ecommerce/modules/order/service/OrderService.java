package com.example.ecommerce.modules.order.service;

import com.example.ecommerce.common.auth.AuthContext;
import com.example.ecommerce.common.auth.UserPrincipal;
import com.example.ecommerce.common.error.ErrorCode;
import com.example.ecommerce.common.exception.BizException;
import com.example.ecommerce.modules.cart.repository.CartRepository;
import com.example.ecommerce.modules.mq.service.AsyncEventService;
import com.example.ecommerce.modules.order.dto.OrderPaymentCallbackResult;
import com.example.ecommerce.modules.order.dto.OrderResponse;
import com.example.ecommerce.modules.order.model.Order;
import com.example.ecommerce.modules.order.model.OrderItem;
import com.example.ecommerce.modules.order.model.OrderStatus;
import com.example.ecommerce.modules.order.repository.OrderRepository;
import com.example.ecommerce.modules.product.model.Product;
import com.example.ecommerce.modules.product.model.ProductStatus;
import com.example.ecommerce.modules.product.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class OrderService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final AsyncEventService asyncEventBus;
    private final long orderExpireSeconds;

    public OrderService(
            CartRepository cartRepository,
            ProductRepository productRepository,
            OrderRepository orderRepository,
            AsyncEventService asyncEventBus,
            @Value("${app.order.expire-seconds:1800}") long orderExpireSeconds
    ) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.asyncEventBus = asyncEventBus;
        this.orderExpireSeconds = orderExpireSeconds;
    }

    public OrderResponse createOrder() {
        Long userId = currentUserId();
        Map<Long, Integer> cartItems = cartRepository.getItems(userId);
        if (cartItems.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "购物车为空，无法下单");
        }

        List<OrderItem> orderItems = new ArrayList<>();
        Map<Long, Integer> reserved = new LinkedHashMap<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (Map.Entry<Long, Integer> entry : cartItems.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .toList()) {
            Long productId = entry.getKey();
            Integer quantity = entry.getValue();
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> {
                        rollbackReservedStock(reserved);
                        return new BizException(ErrorCode.NOT_FOUND, "商品不存在: " + productId);
                    });

            if (product.getStatus() != ProductStatus.ON_SHELF) {
                rollbackReservedStock(reserved);
                throw new BizException(ErrorCode.BAD_REQUEST, "商品未上架，无法下单: " + productId);
            }

            if (!productRepository.reserveStock(productId, quantity)) {
                rollbackReservedStock(reserved);
                throw new BizException(ErrorCode.CONFLICT, "库存不足: " + productId);
            }
            reserved.put(productId, quantity);

            OrderItem orderItem = new OrderItem();
            orderItem.setProductId(productId);
            orderItem.setProductName(product.getName());
            orderItem.setUnitPrice(product.getPrice());
            orderItem.setQuantity(quantity);
            orderItem.setLineAmount(product.getPrice().multiply(BigDecimal.valueOf(quantity)));
            totalAmount = totalAmount.add(orderItem.getLineAmount());
            orderItems.add(orderItem);
        }

        Order order = new Order();
        order.setUserId(userId);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setItems(orderItems);
        order.setTotalAmount(totalAmount);
        order.setExpiresAt(LocalDateTime.now().plusSeconds(orderExpireSeconds));
        orderRepository.save(order);
        asyncEventBus.publishOrderDelayClose(order.getId(), orderExpireSeconds * 1000);

        cartRepository.clear(userId);
        return OrderResponse.from(order);
    }

    public OrderResponse cancelOrder(Long orderId) {
        Order order = findOwnedOrder(orderId, currentUserId());
        closeIfExpired(order);
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new BizException(ErrorCode.CONFLICT, "当前订单状态不可取消: " + order.getStatus());
        }

        releaseOrderStock(order);
        order.setStatus(OrderStatus.CANCELED);
        orderRepository.save(order);
        return OrderResponse.from(order);
    }

    public OrderResponse detail(Long orderId) {
        Order order = findOwnedOrder(orderId, currentUserId());
        closeIfExpired(order);
        return OrderResponse.from(order);
    }

    public List<OrderResponse> myOrders() {
        Long userId = currentUserId();
        return orderRepository.findByUserId(userId)
                .stream()
                .sorted(Comparator.comparing(Order::getId).reversed())
                .peek(this::closeIfExpired)
                .map(OrderResponse::from)
                .toList();
    }

    public OrderResponse completeOrder(Long orderId) {
        Order order = findOwnedOrder(orderId, currentUserId());
        closeIfExpired(order);
        if (order.getStatus() != OrderStatus.PAID) {
            throw new BizException(ErrorCode.CONFLICT, "当前订单状态不可完成: " + order.getStatus());
        }
        order.setStatus(OrderStatus.COMPLETED);
        order.setCompletedAt(LocalDateTime.now());
        orderRepository.save(order);
        return OrderResponse.from(order);
    }

    public OrderPaymentCallbackResult handlePaymentCallback(Long orderId, String transactionId, BigDecimal paidAmount) {
        Order order = findOrderById(orderId);
        closeIfExpired(order);

        if (paidAmount.compareTo(order.getTotalAmount()) != 0) {
            throw new BizException(ErrorCode.CONFLICT, "支付金额与订单金额不一致");
        }

        if (order.getStatus() == OrderStatus.PAID || order.getStatus() == OrderStatus.COMPLETED) {
            if (Objects.equals(order.getPaymentTransactionId(), transactionId)) {
                return new OrderPaymentCallbackResult(order.getId(), order.getStatus(), transactionId, true);
            }
            throw new BizException(ErrorCode.CONFLICT, "订单已支付，回调交易号不一致");
        }

        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new BizException(ErrorCode.CONFLICT, "当前订单状态不可支付: " + order.getStatus());
        }

        order.setStatus(OrderStatus.PAID);
        order.setPaymentTransactionId(transactionId);
        order.setPaidAt(LocalDateTime.now());
        orderRepository.save(order);
        asyncEventBus.publishOrderPaidNotify(order.getId(), transactionId);
        return new OrderPaymentCallbackResult(order.getId(), order.getStatus(), transactionId, false);
    }

    private void closeIfExpired(Order order) {
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            return;
        }
        if (!LocalDateTime.now().isAfter(order.getExpiresAt())) {
            return;
        }

        releaseOrderStock(order);
        order.setStatus(OrderStatus.TIMEOUT_CANCELED);
        orderRepository.save(order);
    }

    private void releaseOrderStock(Order order) {
        for (OrderItem item : order.getItems()) {
            productRepository.releaseStock(item.getProductId(), item.getQuantity());
        }
    }

    private void rollbackReservedStock(Map<Long, Integer> reserved) {
        for (Map.Entry<Long, Integer> entry : reserved.entrySet()) {
            productRepository.releaseStock(entry.getKey(), entry.getValue());
        }
    }

    private Order findOwnedOrder(Long orderId, Long userId) {
        Order order = findOrderById(orderId);
        if (!userId.equals(order.getUserId())) {
            throw new BizException(ErrorCode.NOT_FOUND, "订单不存在: " + orderId);
        }
        return order;
    }

    private Order findOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "订单不存在: " + orderId));
    }

    private Long currentUserId() {
        UserPrincipal principal = AuthContext.get();
        if (principal == null || principal.userId() == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        return principal.userId();
    }
}
