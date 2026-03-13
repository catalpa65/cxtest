package com.example.ecommerce;

import com.example.ecommerce.modules.mq.service.FeishuBotOrderPaidNotificationGateway;
import com.example.ecommerce.modules.order.model.Order;
import com.example.ecommerce.modules.order.repository.OrderRepository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class FeishuBotOrderPaidNotificationGatewayTest {

    @Test
    void shouldSendFeishuTextWebhookPayload() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        AtomicReference<String> contentType = new AtomicReference<>();

        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/hook", exchange -> handle(exchange, requestBody, contentType));
        server.start();
        try {
            String webhookUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/hook";
            FeishuBotOrderPaidNotificationGateway gateway = new FeishuBotOrderPaidNotificationGateway(
                    RestClient.builder(),
                    stubOrderRepository(),
                    webhookUrl,
                    "Payment Succeeded",
                    "ecommerce-practice",
                    "prod",
                    2000,
                    3000
            );

            gateway.send(123L, "tx-abc");

            assertThat(contentType.get()).contains("application/json");
            assertThat(requestBody.get()).contains("\"msg_type\":\"text\"");
            assertThat(requestBody.get()).contains("\"text\":\"Payment Succeeded\\nservice=ecommerce-practice\\nenv=prod\\norderId=123\\namount=88.60\\ntransactionId=tx-abc\\npaidAt=2026-03-13T19:30:00");
        } finally {
            server.stop(0);
        }
    }

    private static OrderRepository stubOrderRepository() {
        return new OrderRepository() {
            @Override
            public Order save(Order order) {
                return order;
            }

            @Override
            public Optional<Order> findById(Long id) {
                Order order = new Order();
                order.setId(id);
                order.setTotalAmount(new BigDecimal("88.60"));
                order.setPaidAt(LocalDateTime.of(2026, 3, 13, 19, 30, 0));
                return Optional.of(order);
            }

            @Override
            public List<Order> findByUserId(Long userId) {
                return List.of();
            }
        };
    }

    private static void handle(
            HttpExchange exchange,
            AtomicReference<String> requestBody,
            AtomicReference<String> contentType
    ) throws IOException {
        requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        contentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
        exchange.sendResponseHeaders(200, -1);
        exchange.close();
    }
}
