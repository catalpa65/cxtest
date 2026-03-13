package com.example.ecommerce.modules.mq.service;

import com.example.ecommerce.modules.mq.model.AsyncEventMessage;
import com.example.ecommerce.modules.mq.model.AsyncEventType;
import org.springframework.stereotype.Component;

@Component
public class OrderPaidNotifyConsumer implements AsyncEventConsumer {

    private final OrderPaidNotificationGateway notificationGateway;

    public OrderPaidNotifyConsumer(OrderPaidNotificationGateway notificationGateway) {
        this.notificationGateway = notificationGateway;
    }

    @Override
    public AsyncEventType supportType() {
        return AsyncEventType.ORDER_PAID_NOTIFY;
    }

    @Override
    public void consume(AsyncEventMessage event) {
        notificationGateway.send(event.getOrderId(), event.getTransactionId());
    }
}
