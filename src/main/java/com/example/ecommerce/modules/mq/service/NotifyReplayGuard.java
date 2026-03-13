package com.example.ecommerce.modules.mq.service;

import com.example.ecommerce.common.error.ErrorCode;
import com.example.ecommerce.common.exception.BizException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class NotifyReplayGuard {

    private final Duration replayWindow;
    private final int maxCacheSize;
    private final ConcurrentHashMap<String, Long> seenNotifyIds = new ConcurrentHashMap<>();

    public NotifyReplayGuard(
            @Value("${app.notify.replay-window-seconds:300}") long replayWindowSeconds,
            @Value("${app.notify.replay-cache-max-size:10000}") int maxCacheSize
    ) {
        this.replayWindow = Duration.ofSeconds(Math.max(1, replayWindowSeconds));
        this.maxCacheSize = Math.max(100, maxCacheSize);
    }

    public void checkAndRecord(String notifyId, Instant timestamp) {
        if (notifyId == null || notifyId.isBlank()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "通知ID缺失");
        }
        if (timestamp == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "通知时间戳缺失");
        }

        long now = Instant.now().toEpochMilli();
        long ts = timestamp.toEpochMilli();
        long diff = Math.abs(now - ts);
        if (diff > replayWindow.toMillis()) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "通知时间戳超出允许窗口");
        }

        cleanupExpired(now);
        Long existed = seenNotifyIds.putIfAbsent(notifyId, ts);
        if (existed != null) {
            throw new BizException(ErrorCode.CONFLICT, "重复通知: " + notifyId);
        }
    }

    private void cleanupExpired(long nowEpochMilli) {
        long expireBefore = nowEpochMilli - replayWindow.toMillis();
        Iterator<Map.Entry<String, Long>> iterator = seenNotifyIds.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Long> entry = iterator.next();
            if (entry.getValue() < expireBefore || seenNotifyIds.size() > maxCacheSize) {
                iterator.remove();
            }
        }
    }
}
