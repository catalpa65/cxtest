package com.example.ecommerce.modules.mq.service;

import com.example.ecommerce.common.error.ErrorCode;
import com.example.ecommerce.common.exception.BizException;
import com.example.ecommerce.modules.mq.model.AsyncEventMessage;
import com.example.ecommerce.modules.mq.model.AsyncEventType;
import com.example.ecommerce.modules.mq.model.AsyncProcessLog;
import com.example.ecommerce.modules.mq.model.AsyncProcessStatus;
import com.example.ecommerce.modules.mq.model.DeadLetterMessage;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Profile("memory")
public class AsyncEventBus implements AsyncEventService {

    private final DelayQueue<QueueTask> queue = new DelayQueue<>();
    private final ConcurrentHashMap<String, DeadLetterMessage> deadLetters = new ConcurrentHashMap<>();
    private final ConcurrentLinkedDeque<AsyncProcessLog> processLogs = new ConcurrentLinkedDeque<>();
    private final Map<AsyncEventType, AsyncEventConsumer> consumers;
    private final int retryMaxAttempts;
    private final long retryDelayMillis;
    private final int processLogRetainSize;
    private final AtomicBoolean running = new AtomicBoolean(false);

    private Thread workerThread;

    public AsyncEventBus(
            List<AsyncEventConsumer> consumers,
            @Value("${app.mq.retry-max-attempts:3}") int retryMaxAttempts,
            @Value("${app.mq.retry-delay-millis:500}") long retryDelayMillis,
            @Value("${app.mq.process-log-retain-size:300}") int processLogRetainSize
    ) {
        this.consumers = consumers.stream()
                .collect(Collectors.toMap(AsyncEventConsumer::supportType, Function.identity()));
        this.retryMaxAttempts = retryMaxAttempts;
        this.retryDelayMillis = retryDelayMillis;
        this.processLogRetainSize = processLogRetainSize;
    }

    @PostConstruct
    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        workerThread = new Thread(this::runLoop, "async-event-bus-worker");
        workerThread.setDaemon(true);
        workerThread.start();
    }

    @PreDestroy
    public void stop() {
        running.set(false);
        if (workerThread != null) {
            workerThread.interrupt();
        }
    }

    public String publishOrderDelayClose(Long orderId, long delayMillis) {
        AsyncEventMessage event = new AsyncEventMessage();
        event.setEventId(UUID.randomUUID().toString());
        event.setType(AsyncEventType.ORDER_DELAY_CLOSE);
        event.setOrderId(orderId);
        event.setAttempts(0);
        event.setCreatedAt(LocalDateTime.now());
        enqueue(event, Math.max(0, delayMillis));
        appendLog(event, AsyncProcessStatus.PUBLISHED, "发布延迟关单事件");
        return event.getEventId();
    }

    public String publishOrderPaidNotify(Long orderId, String transactionId) {
        AsyncEventMessage event = new AsyncEventMessage();
        event.setEventId(UUID.randomUUID().toString());
        event.setType(AsyncEventType.ORDER_PAID_NOTIFY);
        event.setOrderId(orderId);
        event.setTransactionId(transactionId);
        event.setAttempts(0);
        event.setCreatedAt(LocalDateTime.now());
        enqueue(event, 0);
        appendLog(event, AsyncProcessStatus.PUBLISHED, "发布支付成功异步通知事件");
        return event.getEventId();
    }

    public List<DeadLetterMessage> listDeadLetters() {
        return deadLetters.values()
                .stream()
                .sorted(Comparator.comparing(DeadLetterMessage::failedAt).reversed())
                .toList();
    }

    public DeadLetterMessage retryDeadLetter(String eventId) {
        DeadLetterMessage deadLetter = deadLetters.remove(eventId);
        if (deadLetter == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "死信不存在: " + eventId);
        }

        AsyncEventMessage event = new AsyncEventMessage();
        event.setEventId(deadLetter.eventId());
        event.setType(deadLetter.type());
        event.setOrderId(deadLetter.orderId());
        event.setTransactionId(deadLetter.transactionId());
        event.setAttempts(0);
        event.setCreatedAt(LocalDateTime.now());
        enqueue(event, 0);
        appendLog(event, AsyncProcessStatus.MANUAL_RETRY, "手动重试死信");
        return deadLetter;
    }

    public List<AsyncProcessLog> listProcessLogs(int limit) {
        int safeLimit = Math.max(1, limit);
        List<AsyncProcessLog> logs = new ArrayList<>(processLogs);
        Collections.reverse(logs);
        if (logs.size() <= safeLimit) {
            return logs;
        }
        return logs.subList(0, safeLimit);
    }

    private void runLoop() {
        while (running.get()) {
            try {
                QueueTask task = queue.take();
                process(task.message);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception ex) {
                // Protect worker loop from unexpected runtime failures.
            }
        }
    }

    private void process(AsyncEventMessage event) {
        event.setAttempts(event.getAttempts() + 1);
        AsyncEventConsumer consumer = consumers.get(event.getType());
        if (consumer == null) {
            deadLetters.put(
                    event.getEventId(),
                    new DeadLetterMessage(
                            event.getEventId(),
                            event.getType(),
                            event.getOrderId(),
                            event.getTransactionId(),
                            event.getAttempts(),
                            "未找到事件处理器: " + event.getType(),
                            LocalDateTime.now()
                    )
            );
            appendLog(event, AsyncProcessStatus.DEAD_LETTER, "未找到事件处理器");
            return;
        }

        try {
            consumer.consume(event);
            appendLog(event, AsyncProcessStatus.SUCCESS, "处理成功");
        } catch (Exception ex) {
            if (event.getAttempts() < retryMaxAttempts) {
                enqueue(event, retryDelayMillis);
                appendLog(event, AsyncProcessStatus.RETRY_SCHEDULED, "处理失败，准备重试: " + ex.getMessage());
                return;
            }
            deadLetters.put(
                    event.getEventId(),
                    new DeadLetterMessage(
                            event.getEventId(),
                            event.getType(),
                            event.getOrderId(),
                            event.getTransactionId(),
                            event.getAttempts(),
                            ex.getMessage(),
                            LocalDateTime.now()
                    )
            );
            appendLog(event, AsyncProcessStatus.DEAD_LETTER, "超过最大重试次数: " + ex.getMessage());
        }
    }

    private void enqueue(AsyncEventMessage event, long delayMillis) {
        queue.offer(new QueueTask(event, System.currentTimeMillis() + delayMillis));
    }

    private void appendLog(AsyncEventMessage event, AsyncProcessStatus status, String message) {
        processLogs.addLast(
                new AsyncProcessLog(
                        event.getEventId(),
                        event.getType(),
                        event.getOrderId(),
                        event.getTransactionId(),
                        event.getAttempts(),
                        status,
                        message,
                        LocalDateTime.now()
                )
        );
        while (processLogs.size() > processLogRetainSize) {
            processLogs.pollFirst();
        }
    }

    private record QueueTask(AsyncEventMessage message, long executeAtMillis) implements Delayed {

        @Override
        public long getDelay(TimeUnit unit) {
            long diff = executeAtMillis - System.currentTimeMillis();
            return unit.convert(diff, TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(Delayed other) {
            if (other instanceof QueueTask queueTask) {
                return Long.compare(this.executeAtMillis, queueTask.executeAtMillis);
            }
            return 0;
        }
    }
}
