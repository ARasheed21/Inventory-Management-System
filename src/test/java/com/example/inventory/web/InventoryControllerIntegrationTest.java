package com.example.inventory.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
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
        mockMvc.perform(post("/api/inventory/products")
                .with(httpBasic("admin", "admin"))
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
        MvcResult createdResult = mockMvc.perform(post("/api/inventory/products")
                .with(httpBasic("admin", "admin"))
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
                .with(httpBasic("admin", "admin"))
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
        mockMvc.perform(get("/api/products")
                .with(httpBasic("customer", "customer")))
                .andExpect(status().isOk());
    }
}
