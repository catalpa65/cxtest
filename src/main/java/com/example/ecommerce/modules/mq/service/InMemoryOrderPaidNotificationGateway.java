package com.example.ecommerce.modules.mq.service;

import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile("memory")
public class InMemoryOrderPaidNotificationGateway implements OrderPaidNotificationGateway {

    private final ConcurrentHashMap<String, Integer> failTimesByTransaction = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Integer> sentCountByTransaction = new ConcurrentHashMap<>();
    private final List<String> sentRecords = new ArrayList<>();

    @Override
    public synchronized void send(Long orderId, String transactionId) {
        Integer remainFail = failTimesByTransaction.getOrDefault(transactionId, 0);
        if (remainFail > 0) {
            failTimesByTransaction.put(transactionId, remainFail - 1);
            throw new IllegalStateException("模拟通知失败, transactionId=" + transactionId);
        }

        sentCountByTransaction.merge(transactionId, 1, Integer::sum);
        sentRecords.add(LocalDateTime.now() + "|orderId=" + orderId + "|tx=" + transactionId);
    }

    public void setFailTimes(String transactionId, int failTimes) {
        failTimesByTransaction.put(transactionId, Math.max(0, failTimes));
    }

    public int sentCount(String transactionId) {
        return sentCountByTransaction.getOrDefault(transactionId, 0);
    }

    public List<String> sentRecords() {
        return List.copyOf(sentRecords);
    }
}
