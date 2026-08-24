package com.example.inventory.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditHistoryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @Sql(scripts = "/reserved-inventory-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void customersAndWarehouseAreDeniedAuditAccessButReservedReportAllowsWarehouse() throws Exception {
        String customerToken = TestAuthHelper.obtainAccessToken(mockMvc, objectMapper, "customer", "customer");
        String warehouseToken = TestAuthHelper.obtainAccessToken(mockMvc, objectMapper, "warehouse", "warehouse");

        mockMvc.perform(get("/api/admin/audit/orders/ord-res-1")
                .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/audit/orders/ord-res-1")
                .header("Authorization", "Bearer " + warehouseToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/inventory/reserved")
                .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/inventory/reserved")
                .header("Authorization", "Bearer " + warehouseToken))
                .andExpect(status().isOk());
    }

    @Test
    @Sql(scripts = "/order-controller-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void orderAuditHistoryTracksStatusTransitions() throws Exception {
        String customerToken = TestAuthHelper.obtainAccessToken(mockMvc, objectMapper, "customer", "customer");
        String adminToken = TestAuthHelper.obtainAccessToken(mockMvc, objectMapper, "admin", "admin");
        String warehouseToken = TestAuthHelper.obtainAccessToken(mockMvc, objectMapper, "warehouse", "warehouse");

        String createdPayload = mockMvc.perform(post("/api/orders")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "customerId": "customer-001",
                          "items": [
                            {
                              "productId": "SKU-001",
                              "quantity": 1
                            }
                          ]
                        }
                        """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String orderId = objectMapper.readTree(createdPayload).get("id").asText();

        mockMvc.perform(post("/api/orders/{id}/payment", orderId)
                .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/orders/{id}/ship", orderId)
                .header("Authorization", "Bearer " + warehouseToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/audit/orders/{id}", orderId)
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].snapshot.status").value("PENDING"))
                .andExpect(jsonPath("$[1].snapshot.status").value("PAID"))
                .andExpect(jsonPath("$[2].snapshot.status").value("SHIPPED"));
    }

    @Test
    @Sql(scripts = "/order-controller-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void productAuditHistoryListsCreationAndUpdates() throws Exception {
        String adminToken = TestAuthHelper.obtainAccessToken(mockMvc, objectMapper, "admin", "admin");

        String createdPayload = mockMvc.perform(post("/api/inventory/products")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "Audit Widget",
                          "description": "Audited product",
                          "price": "5.00",
                          "currency": "USD",
                          "quantityInStock": 4
                        }
                        """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String productId = objectMapper.readTree(createdPayload).get("id").asText();

        mockMvc.perform(put("/api/inventory/products/{id}", productId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "Audit Widget v2",
                          "description": "Audited product",
                          "price": "6.50",
                          "currency": "USD",
                          "quantityInStock": 9,
                          "category": "tools"
                        }
                        """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/audit/products/{id}", productId)
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].revisionType").value("ADD"))
                .andExpect(jsonPath("$[1].revisionType").value("MOD"))
                .andExpect(jsonPath("$[0].author").isNotEmpty());
    }
}
