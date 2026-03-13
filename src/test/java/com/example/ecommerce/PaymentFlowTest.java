package com.example.ecommerce;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void paymentCallbackShouldBeIdempotentAndSupportCompletion() throws Exception {
        String token = registerAndLogin("pay-" + UUID.randomUUID() + "@example.com");
        CreatedOrder order = createOrderFromCart(token, 1L, 1);

        String transactionId = "txn-" + UUID.randomUUID();
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
                .andExpect(jsonPath("$.data.orderId").value(order.orderId()))
                .andExpect(jsonPath("$.data.status").value("PAID"))
                .andExpect(jsonPath("$.data.idempotent").value(false));

        mockMvc.perform(post("/api/v1/payments/mock-callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(callbackBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.status").value("PAID"))
                .andExpect(jsonPath("$.data.idempotent").value(true));

        mockMvc.perform(post("/api/v1/orders/{orderId}/complete", order.orderId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        mockMvc.perform(post("/api/v1/payments/mock-callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(callbackBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.idempotent").value(true));
    }

    @Test
    void paymentCallbackShouldRejectAmountMismatchAndDifferentTransactionId() throws Exception {
        String token = registerAndLogin("pay-check-" + UUID.randomUUID() + "@example.com");
        CreatedOrder order = createOrderFromCart(token, 2L, 1);

        String badAmountBody = """
                {
                  "orderId": %d,
                  "transactionId": "txn-bad-%s",
                  "paidAmount": 0.01
                }
                """.formatted(order.orderId(), UUID.randomUUID());

        mockMvc.perform(post("/api/v1/payments/mock-callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badAmountBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message", Matchers.containsString("金额不一致")));

        String successTxn = "txn-ok-" + UUID.randomUUID();
        String goodBody = """
                {
                  "orderId": %d,
                  "transactionId": "%s",
                  "paidAmount": %s
                }
                """.formatted(order.orderId(), successTxn, order.totalAmount().toPlainString());

        mockMvc.perform(post("/api/v1/payments/mock-callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(goodBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.status").value("PAID"))
                .andExpect(jsonPath("$.data.idempotent").value(false));

        String anotherTxnBody = """
                {
                  "orderId": %d,
                  "transactionId": "txn-other-%s",
                  "paidAmount": %s
                }
                """.formatted(order.orderId(), UUID.randomUUID(), order.totalAmount().toPlainString());

        mockMvc.perform(post("/api/v1/payments/mock-callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(anotherTxnBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message", Matchers.containsString("交易号不一致")));
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
                  "nickname": "pay-user"
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
