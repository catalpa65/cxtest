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

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.order.expire-seconds=1")
@AutoConfigureMockMvc
class OrderFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createAndCancelOrderShouldReserveAndReleaseStock() throws Exception {
        String token = registerAndLogin("order-" + UUID.randomUUID() + "@example.com");
        int beforeStock = queryProductStock(1L);

        addToCart(token, 1L, 2);

        MvcResult createResult = mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.data.items[0].productId").value(1))
                .andExpect(jsonPath("$.data.items[0].quantity").value(2))
                .andReturn();

        JsonNode createJson = objectMapper.readTree(createResult.getResponse().getContentAsString());
        long orderId = createJson.path("data").path("id").asLong();

        int afterCreateStock = queryProductStock(1L);
        org.junit.jupiter.api.Assertions.assertEquals(beforeStock - 2, afterCreateStock);

        mockMvc.perform(post("/api/v1/orders/{orderId}/cancel", orderId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.status").value("CANCELED"));

        int afterCancelStock = queryProductStock(1L);
        org.junit.jupiter.api.Assertions.assertEquals(beforeStock, afterCancelStock);
    }

    @Test
    void expiredOrderShouldAutoCloseAndRestoreStock() throws Exception {
        String token = registerAndLogin("order-timeout-" + UUID.randomUUID() + "@example.com");
        int beforeStock = queryProductStock(2L);

        addToCart(token, 2L, 1);

        MvcResult createResult = mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.status").value("PENDING_PAYMENT"))
                .andReturn();

        JsonNode createJson = objectMapper.readTree(createResult.getResponse().getContentAsString());
        long orderId = createJson.path("data").path("id").asLong();

        Thread.sleep(1200);

        mockMvc.perform(get("/api/v1/orders/{orderId}", orderId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.status").value("TIMEOUT_CANCELED"));

        int afterTimeoutStock = queryProductStock(2L);
        org.junit.jupiter.api.Assertions.assertEquals(beforeStock, afterTimeoutStock);
    }

    @Test
    void shouldRejectOrderWhenStockInsufficient() throws Exception {
        String token = registerAndLogin("order-stock-" + UUID.randomUUID() + "@example.com");
        long productId = createAndShelfProductWithStock(1);

        addToCart(token, productId, 2);

        mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message", Matchers.containsString("库存不足")));
    }

    private void addToCart(String token, long productId, int quantity) throws Exception {
        String addBody = """
                {
                  "productId": %d,
                  "quantity": %d
                }
                """.formatted(productId, quantity);

        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"));
    }

    private int queryProductStock(Long productId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/products/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.path("data").path("stock").asInt();
    }

    private long createAndShelfProductWithStock(int stock) throws Exception {
        String createBody = """
                {
                  "name": "Limited Product",
                  "description": "for order test",
                  "price": 88.00,
                  "stock": %d
                }
                """.formatted(stock);

        MvcResult createResult = mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andReturn();

        JsonNode createJson = objectMapper.readTree(createResult.getResponse().getContentAsString());
        long productId = createJson.path("data").path("id").asLong();

        mockMvc.perform(put("/api/v1/products/{id}/on-shelf", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"));

        return productId;
    }

    private String registerAndLogin(String email) throws Exception {
        String registerBody = """
                {
                  "email": "%s",
                  "password": "Password123",
                  "nickname": "order-user"
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
}
