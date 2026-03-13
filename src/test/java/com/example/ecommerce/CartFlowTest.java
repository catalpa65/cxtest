package com.example.ecommerce;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CartFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void cartCrudShouldWorkForLoggedInUser() throws Exception {
        String token = registerAndLogin("cart-" + UUID.randomUUID() + "@example.com");

        String addBody = """
                {
                  "productId": 1,
                  "quantity": 2
                }
                """;

        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.totalQuantity").value(2))
                .andExpect(jsonPath("$.data.items[0].productId").value(1))
                .andExpect(jsonPath("$.data.items[0].quantity").value(2));

        String updateBody = """
                {
                  "quantity": 3
                }
                """;

        mockMvc.perform(put("/api/v1/cart/items/1")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.totalQuantity").value(3))
                .andExpect(jsonPath("$.data.items[0].quantity").value(3));

        mockMvc.perform(get("/api/v1/cart")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.totalQuantity").value(3));

        mockMvc.perform(delete("/api/v1/cart/items/1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.totalQuantity").value(0))
                .andExpect(jsonPath("$.data.items.length()").value(0));
    }

    @Test
    void cartShouldRejectUnauthenticatedAndDraftProduct() throws Exception {
        mockMvc.perform(get("/api/v1/cart"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        String token = registerAndLogin("cart-draft-" + UUID.randomUUID() + "@example.com");

        String createProductBody = """
                {
                  "name": "Draft Product",
                  "description": "not on shelf",
                  "price": 199.00
                }
                """;

        MvcResult createProductResult = mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createProductBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andReturn();

        JsonNode createProductJson = objectMapper.readTree(createProductResult.getResponse().getContentAsString());
        long productId = createProductJson.path("data").path("id").asLong();

        String addBody = """
                {
                  "productId": %d,
                  "quantity": 1
                }
                """.formatted(productId);

        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message", containsString("未上架")));
    }

    private String registerAndLogin(String email) throws Exception {
        String registerBody = """
                {
                  "email": "%s",
                  "password": "Password123",
                  "nickname": "cart-user"
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
