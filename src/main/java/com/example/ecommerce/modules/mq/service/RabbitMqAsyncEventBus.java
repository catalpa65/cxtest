package com.example.ecommerce.modules.mq.service;

import com.example.ecommerce.common.error.ErrorCode;
import com.example.ecommerce.common.exception.BizException;
import com.example.ecommerce.modules.mq.config.RabbitMqConfig;
import com.example.ecommerce.modules.mq.model.AsyncEventMessage;
import com.example.ecommerce.modules.mq.model.AsyncEventType;
import com.example.ecommerce.modules.mq.model.AsyncProcessLog;
import com.example.ecommerce.modules.mq.model.AsyncProcessStatus;
import com.example.ecommerce.modules.mq.model.DeadLetterMessage;
import com.example.ecommerce.modules.mq.repository.mysql.MqDeadLetterMapper;
import com.example.ecommerce.modules.mq.repository.mysql.MqProcessLogMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Profile("prod")
public class RabbitMqAsyncEventBus implements AsyncEventService {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final MqProcessLogMapper mqProcessLogMapper;
    private final MqDeadLetterMapper mqDeadLetterMapper;
    private final Map<AsyncEventType, AsyncEventConsumer> consumers;
    private final int retryMaxAttempts;
    private final long retryDelayMillis;
    private final int processLogRetainSize;

    public RabbitMqAsyncEventBus(
            RabbitTemplate rabbitTemplate,
            ObjectMapper objectMapper,
            MqProcessLogMapper mqProcessLogMapper,
            MqDeadLetterMapper mqDeadLetterMapper,
            List<AsyncEventConsumer> consumers,
            @Value("${app.mq.retry-max-attempts:3}") int retryMaxAttempts,
            @Value("${app.mq.retry-delay-millis:500}") long retryDelayMillis,
            @Value("${app.mq.process-log-retain-size:300}") int processLogRetainSize
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.mqProcessLogMapper = mqProcessLogMapper;
        this.mqDeadLetterMapper = mqDeadLetterMapper;
        this.retryMaxAttempts = retryMaxAttempts;
        this.retryDelayMillis = retryDelayMillis;
        this.processLogRetainSize = processLogRetainSize;
        this.consumers = consumers.stream()
                .collect(Collectors.toMap(AsyncEventConsumer::supportType, Function.identity()));
    }

    @Override
    public String publishOrderDelayClose(Long orderId, long delayMillis) {
        AsyncEventMessage event = new AsyncEventMessage();
        event.setEventId(UUID.randomUUID().toString());
        event.setType(AsyncEventType.ORDER_DELAY_CLOSE);
        event.setOrderId(orderId);
        event.setAttempts(0);
        event.setCreatedAt(LocalDateTime.now());
        publish(event, Math.max(0, delayMillis));
        appendLog(event, AsyncProcessStatus.PUBLISHED, "发布延迟关单事件");
        return event.getEventId();
    }

    @Override
    public String publishOrderPaidNotify(Long orderId, String transactionId) {
        AsyncEventMessage event = new AsyncEventMessage();
        event.setEventId(UUID.randomUUID().toString());
        event.setType(AsyncEventType.ORDER_PAID_NOTIFY);
        event.setOrderId(orderId);
        event.setTransactionId(transactionId);
        event.setAttempts(0);
        event.setCreatedAt(LocalDateTime.now());
        publish(event, 0);
        appendLog(event, AsyncProcessStatus.PUBLISHED, "发布支付成功异步通知事件");
        return event.getEventId();
    }

    @Override
    public List<DeadLetterMessage> listDeadLetters() {
        return mqDeadLetterMapper.listAll();
    }

    @Override
    public DeadLetterMessage retryDeadLetter(String eventId) {
        DeadLetterMessage deadLetter = mqDeadLetterMapper.findByEventId(eventId);
        if (deadLetter == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "死信不存在: " + eventId);
        }
        mqDeadLetterMapper.deleteByEventId(eventId);

        AsyncEventMessage event = new AsyncEventMessage();
        event.setEventId(deadLetter.eventId());
        event.setType(deadLetter.type());
        event.setOrderId(deadLetter.orderId());
        event.setTransactionId(deadLetter.transactionId());
        event.setAttempts(0);
        event.setCreatedAt(LocalDateTime.now());
        publish(event, 0);
        appendLog(event, AsyncProcessStatus.MANUAL_RETRY, "手动重试死信");
        return deadLetter;
    }

    @Override
    public List<AsyncProcessLog> listProcessLogs(int limit) {
        int safeLimit = Math.max(1, limit);
        return mqProcessLogMapper.listLatest(safeLimit);
    }

    @RabbitListener(queues = RabbitMqConfig.MAIN_QUEUE)
    public void handleMainQueueMessage(String payload) {
        AsyncEventMessage event = parse(payload);
        if (event == null || event.getType() == null || event.getEventId() == null) {
            return;
        }

        event.setAttempts(event.getAttempts() + 1);
        AsyncEventConsumer consumer = consumers.get(event.getType());
        if (consumer == null) {
            moveToDeadLetter(event, "未找到事件处理器: " + event.getType());
            return;
        }

        try {
            consumer.consume(event);
            appendLog(event, AsyncProcessStatus.SUCCESS, "处理成功");
        } catch (Exception ex) {
            if (event.getAttempts() < retryMaxAttempts) {
                publish(event, retryDelayMillis);
                appendLog(event, AsyncProcessStatus.RETRY_SCHEDULED, "处理失败，准备重试: " + ex.getMessage());
                return;
            }
            moveToDeadLetter(event, ex.getMessage());
        }
    }

    private void moveToDeadLetter(AsyncEventMessage event, String errorMessage) {
        DeadLetterMessage deadLetter = new DeadLetterMessage(
                event.getEventId(),
                event.getType(),
                event.getOrderId(),
                event.getTransactionId(),
                event.getAttempts(),
                errorMessage,
                LocalDateTime.now()
        );
        mqDeadLetterMapper.upsert(deadLetter);
        appendLog(event, AsyncProcessStatus.DEAD_LETTER, "超过最大重试次数: " + errorMessage);
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.DEAD_EXCHANGE,
                RabbitMqConfig.DEAD_ROUTING_KEY,
                encode(event)
        );
    }

    private void publish(AsyncEventMessage event, long delayMillis) {
        String payload = encode(event);
        if (delayMillis <= 0) {
            rabbitTemplate.convertAndSend(RabbitMqConfig.MAIN_EXCHANGE, RabbitMqConfig.MAIN_ROUTING_KEY, payload);
            return;
        }
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.DELAY_EXCHANGE,
                RabbitMqConfig.DELAY_ROUTING_KEY,
                payload,
                message -> {
                    message.getMessageProperties().setExpiration(Long.toString(delayMillis));
                    return message;
                }
        );
    }

    private void appendLog(AsyncEventMessage event, AsyncProcessStatus status, String message) {
        AsyncProcessLog log = new AsyncProcessLog(
                event.getEventId(),
                event.getType(),
                event.getOrderId(),
                event.getTransactionId(),
                event.getAttempts(),
                status,
                message,
                LocalDateTime.now()
        );
        mqProcessLogMapper.insert(log);
        if (processLogRetainSize > 0) {
            mqProcessLogMapper.trimToRetainSize(processLogRetainSize);
        }
    }

    private String encode(AsyncEventMessage event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("序列化异步事件失败", ex);
        }
    }

    private AsyncEventMessage parse(String payload) {
        if (payload == null || payload.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(payload, AsyncEventMessage.class);
        } catch (JsonProcessingException ex) {
            return null;
        }
    }
}
