package com.example.ecommerce.modules.mq.service;

import com.example.ecommerce.modules.mq.model.AsyncProcessLog;
import com.example.ecommerce.modules.mq.model.DeadLetterMessage;

import java.util.List;

public interface AsyncEventService {

    String publishOrderDelayClose(Long orderId, long delayMillis);

    String publishOrderPaidNotify(Long orderId, String transactionId);

    List<DeadLetterMessage> listDeadLetters();

    DeadLetterMessage retryDeadLetter(String eventId);

    List<AsyncProcessLog> listProcessLogs(int limit);
}
