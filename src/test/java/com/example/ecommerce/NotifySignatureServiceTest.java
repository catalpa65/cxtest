package com.example.ecommerce;

import com.example.ecommerce.modules.mq.service.NotifySignatureService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NotifySignatureServiceTest {

    @Test
    void shouldSignAndVerify() {
        NotifySignatureService service = new NotifySignatureService("unit-test-secret");
        String timestamp = "2026-03-11T18:30:00Z";
        String notifyId = "notify-1";
        Long orderId = 123L;
        String transactionId = "tx-abc";
        String sentAt = "2026-03-11T18:29:59Z";

        String signature = service.sign(timestamp, notifyId, orderId, transactionId, sentAt);
        boolean valid = service.verify(timestamp, notifyId, orderId, transactionId, sentAt, signature);

        assertThat(valid).isTrue();
    }

    @Test
    void shouldRejectInvalidSignature() {
        NotifySignatureService service = new NotifySignatureService("unit-test-secret");
        String timestamp = "2026-03-11T18:30:00Z";
        String notifyId = "notify-1";
        Long orderId = 123L;
        String transactionId = "tx-abc";
        String sentAt = "2026-03-11T18:29:59Z";

        String signature = service.sign(timestamp, notifyId, orderId, transactionId, sentAt);
        boolean valid = service.verify(timestamp, "notify-2", orderId, transactionId, sentAt, signature);

        assertThat(valid).isFalse();
    }
}
