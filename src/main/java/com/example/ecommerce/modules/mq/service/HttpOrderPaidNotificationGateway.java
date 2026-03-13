package com.example.ecommerce.modules.mq.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.UUID;

@Component
@Profile("prod")
@ConditionalOnProperty(name = "app.notify.provider", havingValue = "mock", matchIfMissing = true)
public class HttpOrderPaidNotificationGateway implements OrderPaidNotificationGateway {

    private static final String HEADER_NOTIFY_TIMESTAMP = "X-Notify-Timestamp";
    private static final String HEADER_NOTIFY_ID = "X-Notify-Id";
    private static final String HEADER_NOTIFY_SIGNATURE = "X-Notify-Signature";

    private final RestClient restClient;
    private final String orderPaidNotifyUrl;
    private final NotifySignatureService notifySignatureService;

    public HttpOrderPaidNotificationGateway(
            RestClient.Builder restClientBuilder,
            NotifySignatureService notifySignatureService,
            @Value("${app.notify.order-paid-url:http://127.0.0.1:8080/api/v1/notify/order-paid}") String orderPaidNotifyUrl,
            @Value("${app.notify.connect-timeout-millis:2000}") int connectTimeoutMillis,
            @Value("${app.notify.read-timeout-millis:3000}") int readTimeoutMillis
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeoutMillis);
        requestFactory.setReadTimeout(readTimeoutMillis);
        this.restClient = restClientBuilder
                .requestFactory(requestFactory)
                .build();
        this.notifySignatureService = notifySignatureService;
        this.orderPaidNotifyUrl = orderPaidNotifyUrl;
    }

    @Override
    public void send(Long orderId, String transactionId) {
        String sentAt = Instant.now().toString();
        NotifyOrderPaidPayload payload = new NotifyOrderPaidPayload(orderId, transactionId, sentAt);
        String timestamp = Instant.now().toString();
        String notifyId = UUID.randomUUID().toString();
        String signature = notifySignatureService.sign(timestamp, notifyId, orderId, transactionId, sentAt);
        restClient.post()
                .uri(orderPaidNotifyUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HEADER_NOTIFY_TIMESTAMP, timestamp)
                .header(HEADER_NOTIFY_ID, notifyId)
                .header(HEADER_NOTIFY_SIGNATURE, signature)
                .body(payload)
                .retrieve()
                .toBodilessEntity();
    }

    private record NotifyOrderPaidPayload(
            Long orderId,
            String transactionId,
            String sentAt
    ) {
    }
}
