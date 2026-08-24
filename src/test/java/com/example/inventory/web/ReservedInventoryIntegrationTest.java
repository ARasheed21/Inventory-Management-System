package com.example.inventory.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReservedInventoryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @Sql(scripts = "/reserved-inventory-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void reservedQuantityDropsWhenPendingOrderLeavesPendingState() throws Exception {
        String adminToken = TestAuthHelper.obtainAccessToken(mockMvc, objectMapper, "admin", "admin");
        String customerToken = TestAuthHelper.obtainAccessToken(mockMvc, objectMapper, "customer", "customer");

        mockMvc.perform(post("/api/orders/{id}/payment", "ord-res-1")
                .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/inventory/reserved")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].quantityReserved").value(0))
                .andExpect(jsonPath("$[0].quantityAvailable").value(10));
    }

    @Test
    @Sql(scripts = "/reserved-inventory-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void reservedInventoryReportReflectsPendingOrderAllocations() throws Exception {
        String adminToken = TestAuthHelper.obtainAccessToken(mockMvc, objectMapper, "admin", "admin");

        mockMvc.perform(get("/api/inventory/reserved")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productId").value("SKU-001"))
                .andExpect(jsonPath("$[0].quantityInStock").value(10))
                .andExpect(jsonPath("$[0].quantityReserved").value(3))
                .andExpect(jsonPath("$[0].quantityAvailable").value(7));
    }
}
