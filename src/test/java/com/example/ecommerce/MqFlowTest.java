package com.example.ecommerce;

import com.example.ecommerce.modules.mq.service.InMemoryOrderPaidNotificationGateway;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.order.expire-seconds=1",
        "app.mq.retry-max-attempts=3",
        "app.mq.retry-delay-millis=100"
})
@AutoConfigureMockMvc
class MqFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InMemoryOrderPaidNotificationGateway notificationGateway;

    @Test
    void delayedCloseShouldBeHandledByAsyncEvent() throws Exception {
        String token = registerAndLogin("mq-delay-" + UUID.randomUUID() + "@example.com");
        CreatedOrder order = createOrderFromCart(token, 1L, 1);

        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    mockMvc.perform(get("/api/v1/orders/{orderId}", order.orderId())
                                    .header("Authorization", "Bearer " + token))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.data.status").value("TIMEOUT_CANCELED"));
                });

        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    JsonNode logs = processLogsJson();
                    boolean matched = false;
                    for (JsonNode item : logs) {
                        if (item.path("type").asText().equals("ORDER_DELAY_CLOSE")
                                && item.path("orderId").asLong() == order.orderId()
                                && item.path("status").asText().equals("SUCCESS")) {
                            matched = true;
                            break;
                        }
                    }
                    assertThat(matched).isTrue();
                });
    }

    @Test
    void deadLetterShouldSupportManualRetry() throws Exception {
        String token = registerAndLogin("mq-dlq-" + UUID.randomUUID() + "@example.com");
        CreatedOrder order = createOrderFromCart(token, 2L, 1);

        String transactionId = "mq-tx-" + UUID.randomUUID();
        notificationGateway.setFailTimes(transactionId, 10);

        String callbackBody = """
                {
                  "orderId": %d,
                  "transactionId": "%s",
                  "paidAmount": %s
                }
                """.formatted(order.orderId(), transactionId, order.totalAmount().toPlainString());

        mockMvc.perform(post("/api/v1/payments/mock-callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(callbackBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.status").value("PAID"));

        String deadEventId = Awaitility.await()
                .atMost(Duration.ofSeconds(8))
                .until(() -> findDeadLetterEventId(transactionId), id -> id != null);

        notificationGateway.setFailTimes(transactionId, 0);

        mockMvc.perform(post("/api/v1/mq/dead-letters/{eventId}/retry", deadEventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.eventId").value(deadEventId));

        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    assertThat(findDeadLetterEventId(transactionId)).isNull();
                    assertThat(notificationGateway.sentCount(transactionId)).isGreaterThan(0);
                });
    }

    private String findDeadLetterEventId(String transactionId) throws Exception {
        MvcResult deadLettersResult = mockMvc.perform(get("/api/v1/mq/dead-letters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andReturn();
        JsonNode root = objectMapper.readTree(deadLettersResult.getResponse().getContentAsString());
        for (JsonNode item : root.path("data")) {
            if (transactionId.equals(item.path("transactionId").asText())) {
                return item.path("eventId").asText();
            }
        }
        return null;
    }

    private JsonNode processLogsJson() throws Exception {
        MvcResult logsResult = mockMvc.perform(get("/api/v1/mq/process-logs")
                        .param("limit", "200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andReturn();
        JsonNode root = objectMapper.readTree(logsResult.getResponse().getContentAsString());
        return root.path("data");
    }

    private CreatedOrder createOrderFromCart(String token, long productId, int quantity) throws Exception {
        String addCartBody = """
                {
                  "productId": %d,
                  "quantity": %d
                }
                """.formatted(productId, quantity);

        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addCartBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"));

        MvcResult createOrderResult = mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.status").value("PENDING_PAYMENT"))
                .andReturn();

        JsonNode createJson = objectMapper.readTree(createOrderResult.getResponse().getContentAsString());
        long orderId = createJson.path("data").path("id").asLong();
        BigDecimal totalAmount = createJson.path("data").path("totalAmount").decimalValue();
        return new CreatedOrder(orderId, totalAmount);
    }

    private String registerAndLogin(String email) throws Exception {
        String registerBody = """
                {
                  "email": "%s",
                  "password": "Password123",
                  "nickname": "mq-user"
                }
                """.formatted(email);

        mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"));

        String loginBody = """
                {
                  "email": "%s",
                  "password": "Password123"
                }
                """.formatted(email);

        MvcResult loginResult = mockMvc.perform(post("/api/v1/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        return loginJson.path("data").path("token").asText();
    }

    private record CreatedOrder(long orderId, BigDecimal totalAmount) {
    }
}
