package com.example.ecommerce.modules.mq.service;

public interface OrderPaidNotificationGateway {

    void send(Long orderId, String transactionId);
}
