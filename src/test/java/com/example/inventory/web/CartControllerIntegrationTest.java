package com.example.inventory.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CartControllerIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @Test
        @Sql(scripts = "/cart-controller-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
        void shouldAddUpdateAndDeleteCartItemsForAuthenticatedCustomer() throws Exception {
                String accessToken = TestAuthHelper.obtainAccessToken(mockMvc, objectMapper, "customer", "customer");

                MvcResult addResult = mockMvc.perform(post("/api/cart")
                                .header("Authorization", "Bearer " + accessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "productId": "SKU-001",
                                                  "quantity": 2
                                                }
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.productId").value("SKU-001"))
                                .andExpect(jsonPath("$.quantity").value(2))
                                .andReturn();

                String cartItemId = objectMapper.readTree(addResult.getResponse().getContentAsString()).get("id")
                                .asText();

                mockMvc.perform(get("/api/cart")
                                .header("Authorization", "Bearer " + accessToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].id").value(cartItemId))
                                .andExpect(jsonPath("$[0].quantity").value(2));

                mockMvc.perform(put("/api/cart/{itemId}", cartItemId)
                                .header("Authorization", "Bearer " + accessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "productId": "SKU-001",
                                                  "quantity": 5
                                                }
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.quantity").value(5));

                mockMvc.perform(delete("/api/cart/{itemId}", cartItemId)
                                .header("Authorization", "Bearer " + accessToken))
                                .andExpect(status().isNoContent());

                mockMvc.perform(get("/api/cart")
                                .header("Authorization", "Bearer " + accessToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        void shouldRejectUnauthenticatedCartAccess() throws Exception {
                mockMvc.perform(get("/api/cart"))
                                .andExpect(status().isUnauthorized());
        }
}
