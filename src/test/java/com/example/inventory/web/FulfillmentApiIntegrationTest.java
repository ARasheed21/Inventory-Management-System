package com.example.inventory.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class FulfillmentApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String createAndPayOrderAsCustomer() throws Exception {
        String customerToken = TestAuthHelper.obtainAccessToken(mockMvc, objectMapper, "customer", "customer");

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

        return orderId;
    }

    @Test
    void fulfillmentOfUnknownOrderReturnsNotFound() throws Exception {
        String warehouseToken = TestAuthHelper.obtainAccessToken(mockMvc, objectMapper, "warehouse", "warehouse");

        mockMvc.perform(post("/api/orders/{id}/ship", "no-such-order")
                .header("Authorization", "Bearer " + warehouseToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/orders/{id}/deliver", "no-such-order")
                .header("Authorization", "Bearer " + warehouseToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @Sql(scripts = "/order-controller-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void invalidFulfillmentTransitionsAreRejectedAsConflict() throws Exception {
        String warehouseToken = TestAuthHelper.obtainAccessToken(mockMvc, objectMapper, "warehouse", "warehouse");

        String customerToken = TestAuthHelper.obtainAccessToken(mockMvc, objectMapper, "customer", "customer");
        String pendingOrderId = mockMvc.perform(post("/api/orders")
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
        String orderId = objectMapper.readTree(pendingOrderId).get("id").asText();

        mockMvc.perform(post("/api/orders/{id}/ship", orderId)
                .header("Authorization", "Bearer " + warehouseToken))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/orders/{id}/deliver", orderId)
                .header("Authorization", "Bearer " + warehouseToken))
                .andExpect(status().isConflict());
    }

    @Test
    @Sql(scripts = "/order-controller-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void customerAndAnonymousCannotShipOrders() throws Exception {
        String orderId = createAndPayOrderAsCustomer();
        String customerToken = TestAuthHelper.obtainAccessToken(mockMvc, objectMapper, "customer", "customer");

        mockMvc.perform(post("/api/orders/{id}/ship", orderId)
                .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/orders/{id}/ship", orderId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Sql(scripts = "/order-controller-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void warehouseCanShipPaidOrderThenDeliverIt() throws Exception {
        String warehouseToken = TestAuthHelper.obtainAccessToken(mockMvc, objectMapper, "warehouse", "warehouse");
        String orderId = createAndPayOrderAsCustomer();

        mockMvc.perform(post("/api/orders/{id}/ship", orderId)
                .header("Authorization", "Bearer " + warehouseToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHIPPED"));

        mockMvc.perform(post("/api/orders/{id}/deliver", orderId)
                .header("Authorization", "Bearer " + warehouseToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED"));
    }
}
