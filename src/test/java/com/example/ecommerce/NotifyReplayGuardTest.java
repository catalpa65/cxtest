package com.example.ecommerce;

import com.example.ecommerce.common.exception.BizException;
import com.example.ecommerce.modules.mq.service.NotifyReplayGuard;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotifyReplayGuardTest {

    @Test
    void shouldAcceptFreshUniqueNotifyId() {
        NotifyReplayGuard guard = new NotifyReplayGuard(300, 1000);
        assertThatCode(() -> guard.checkAndRecord("notify-1", Instant.now())).doesNotThrowAnyException();
    }

    @Test
    void shouldRejectDuplicateNotifyId() {
        NotifyReplayGuard guard = new NotifyReplayGuard(300, 1000);
        guard.checkAndRecord("notify-1", Instant.now());
        assertThatThrownBy(() -> guard.checkAndRecord("notify-1", Instant.now()))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("重复通知");
    }

    @Test
    void shouldRejectExpiredTimestamp() {
        NotifyReplayGuard guard = new NotifyReplayGuard(5, 1000);
        Instant oldTs = Instant.now().minusSeconds(10);
        assertThatThrownBy(() -> guard.checkAndRecord("notify-old", oldTs))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("超出允许窗口");
    }
}
