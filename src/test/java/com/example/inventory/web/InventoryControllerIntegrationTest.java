package com.example.inventory.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
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
class InventoryControllerIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @Test
        void adminCanCreateProductThroughInventoryEndpoint() throws Exception {
                String accessToken = TestAuthHelper.obtainAccessToken(mockMvc, objectMapper, "admin", "admin");

                mockMvc.perform(post("/api/inventory/products")
                                .header("Authorization", "Bearer " + accessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "name": "Desk Lamp",
                                                  "description": "Compact desk lamp",
                                                  "price": "45.00",
                                                  "currency": "USD",
                                                  "quantityInStock": 25
                                                }
                                                """))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.name").value("Desk Lamp"))
                                .andExpect(jsonPath("$.price").value("45.00"))
                                .andExpect(jsonPath("$.currency").value("USD"));
        }

        @Test
        void adminCanUpdateExistingProductThroughInventoryEndpoint() throws Exception {
                String accessToken = TestAuthHelper.obtainAccessToken(mockMvc, objectMapper, "admin", "admin");

                MvcResult createdResult = mockMvc.perform(post("/api/inventory/products")
                                .header("Authorization", "Bearer " + accessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "name": "Notebook",
                                                  "description": "Paper notebook",
                                                  "price": "12.50",
                                                  "currency": "USD",
                                                  "quantityInStock": 18
                                                }
                                                """))
                                .andExpect(status().isCreated())
                                .andReturn();

                JsonNode createdPayload = objectMapper.readTree(createdResult.getResponse().getContentAsString());
                String existingProductId = createdPayload.get("id").asText();

                mockMvc.perform(put("/api/inventory/products/{id}", existingProductId)
                                .header("Authorization", "Bearer " + accessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "name": "Notebook Pro",
                                                  "description": "Updated paper notebook",
                                                  "price": "14.90",
                                                  "currency": "USD",
                                                  "quantityInStock": 12
                                                }
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.name").value("Notebook Pro"))
                                .andExpect(jsonPath("$.quantityInStock").value(12));
        }

        @Test
        void customerCanBrowseAvailableProducts() throws Exception {
                String accessToken = TestAuthHelper.obtainAccessToken(mockMvc, objectMapper, "customer", "customer");

                mockMvc.perform(get("/api/products")
                                .header("Authorization", "Bearer " + accessToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content").isArray())
                                .andExpect(jsonPath("$.page").value(0))
                                .andExpect(jsonPath("$.size").value(20));
        }

        @Test
        @Sql(scripts = "/inventory-controller-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
        void customerCanFilterProductsBySearchAndCategory() throws Exception {
                String accessToken = TestAuthHelper.obtainAccessToken(mockMvc, objectMapper, "admin", "admin");

                for (int i = 1; i <= 3; i++) {
                        mockMvc.perform(post("/api/inventory/products")
                                        .header("Authorization", "Bearer " + accessToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("""
                                                        {
                                                          "name": "Lamp Model %d",
                                                          "description": "A lamp",
                                                          "price": "10.00",
                                                          "currency": "USD",
                                                          "quantityInStock": 5,
                                                          "category": "lighting"
                                                        }
                                                        """.formatted(i)))
                                        .andExpect(status().isCreated());
                }

                mockMvc.perform(post("/api/inventory/products")
                                .header("Authorization", "Bearer " + accessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "name": "Office Chair",
                                                  "description": "Ergonomic chair",
                                                  "price": "99.00",
                                                  "currency": "USD",
                                                  "quantityInStock": 3,
                                                  "category": "furniture"
                                                }
                                                """))
                                .andExpect(status().isCreated());

                String customerToken = TestAuthHelper.obtainAccessToken(mockMvc, objectMapper, "customer", "customer");

                mockMvc.perform(get("/api/products")
                                .header("Authorization", "Bearer " + customerToken)
                                .param("search", "lamp"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.total").value(3));

                mockMvc.perform(get("/api/products")
                                .header("Authorization", "Bearer " + customerToken)
                                .param("category", "furniture"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.total").value(1))
                                .andExpect(jsonPath("$.content[0].name").value("Office Chair"));

                mockMvc.perform(get("/api/products")
                                .header("Authorization", "Bearer " + customerToken)
                                .param("page", "0")
                                .param("size", "2"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content.length()").value(2))
                                .andExpect(jsonPath("$.total").value(4));
        }

        @Test
        @Sql(scripts = "/inventory-controller-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
        void productDetailEndpointReturnsProductOr404() throws Exception {
                String accessToken = TestAuthHelper.obtainAccessToken(mockMvc, objectMapper, "admin", "admin");

                MvcResult createdResult = mockMvc.perform(post("/api/inventory/products")
                                .header("Authorization", "Bearer " + accessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "name": "Monitor",
                                                  "description": "4K monitor",
                                                  "price": "249.00",
                                                  "currency": "USD",
                                                  "quantityInStock": 7,
                                                  "category": "electronics"
                                                }
                                                """))
                                .andExpect(status().isCreated())
                                .andReturn();

                String productId = objectMapper.readTree(createdResult.getResponse().getContentAsString()).get("id")
                                .asText();

                String customerToken = TestAuthHelper.obtainAccessToken(mockMvc, objectMapper, "customer", "customer");
                mockMvc.perform(get("/api/products/{id}", productId)
                                .header("Authorization", "Bearer " + customerToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(productId))
                                .andExpect(jsonPath("$.category").value("electronics"));

                mockMvc.perform(get("/api/products/{id}", "does-not-exist")
                                .header("Authorization", "Bearer " + customerToken))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.status").value(404))
                                .andExpect(jsonPath("$.message").value("Product not found: does-not-exist"));
        }
}
