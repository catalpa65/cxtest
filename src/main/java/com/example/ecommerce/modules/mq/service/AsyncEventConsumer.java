package com.example.ecommerce.modules.mq.service;

import com.example.ecommerce.modules.mq.model.AsyncEventMessage;
import com.example.ecommerce.modules.mq.model.AsyncEventType;

public interface AsyncEventConsumer {

    AsyncEventType supportType();

    void consume(AsyncEventMessage event);
}
