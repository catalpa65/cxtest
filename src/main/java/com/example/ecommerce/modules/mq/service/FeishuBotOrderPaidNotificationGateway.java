package com.example.ecommerce.modules.mq.service;

import com.example.ecommerce.modules.order.model.Order;
import com.example.ecommerce.modules.order.repository.OrderRepository;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Component
@Profile("prod")
@ConditionalOnProperty(name = "app.notify.provider", havingValue = "feishu")
public class FeishuBotOrderPaidNotificationGateway implements OrderPaidNotificationGateway {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final RestClient restClient;
    private final OrderRepository orderRepository;
    private final String webhookUrl;
    private final String title;
    private final String applicationName;
    private final String envLabel;

    public FeishuBotOrderPaidNotificationGateway(
            RestClient.Builder restClientBuilder,
            OrderRepository orderRepository,
            @Value("${app.notify.feishu-webhook-url:}") String webhookUrl,
            @Value("${app.notify.feishu-title:Payment Succeeded}") String title,
            @Value("${spring.application.name:ecommerce-practice}") String applicationName,
            @Value("${app.notify.feishu-env-label:prod}") String envLabel,
            @Value("${app.notify.connect-timeout-millis:2000}") int connectTimeoutMillis,
            @Value("${app.notify.read-timeout-millis:3000}") int readTimeoutMillis
    ) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            throw new IllegalStateException("Feishu webhook URL is required when app.notify.provider=feishu");
        }

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeoutMillis);
        requestFactory.setReadTimeout(readTimeoutMillis);

        this.restClient = restClientBuilder
                .requestFactory(requestFactory)
                .build();
        this.orderRepository = orderRepository;
        this.webhookUrl = webhookUrl;
        this.title = title;
        this.applicationName = applicationName;
        this.envLabel = envLabel;
    }

    @Override
    public void send(Long orderId, String transactionId) {
        String sentAt = Instant.now().toString();
        Optional<Order> order = orderRepository.findById(orderId);
        FeishuWebhookMessage body = new FeishuWebhookMessage(
                "text",
                new FeishuTextContent(buildText(orderId, transactionId, sentAt, order.orElse(null)))
        );

        restClient.post()
                .uri(webhookUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    String buildText(Long orderId, String transactionId, String sentAt, Order order) {
        return title
                + "\nservice=" + applicationName
                + "\nenv=" + envLabel
                + "\norderId=" + orderId
                + "\namount=" + orderAmount(order)
                + "\ntransactionId=" + transactionId
                + "\npaidAt=" + formatTime(order == null ? null : order.getPaidAt())
                + "\nsentAt=" + sentAt;
    }

    private String orderAmount(Order order) {
        if (order == null || order.getTotalAmount() == null) {
            return "unknown";
        }
        return order.getTotalAmount().toPlainString();
    }

    private String formatTime(LocalDateTime time) {
        if (time == null) {
            return "unknown";
        }
        return TIME_FORMATTER.format(time);
    }

    private record FeishuWebhookMessage(
            @JsonProperty("msg_type") String msgType,
            FeishuTextContent content
    ) {
    }

    private record FeishuTextContent(String text) {
    }
}
