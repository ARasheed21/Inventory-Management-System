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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderControllerIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  @Sql(scripts = "/order-controller-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  void orderCreationEndpointReturnsCreatedResponse() throws Exception {
    String accessToken = TestAuthHelper.obtainAccessToken(mockMvc, objectMapper, "customer", "customer");

    mockMvc.perform(post("/api/orders")
        .header("Authorization", "Bearer " + accessToken)
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "customerId": "customer-001",
              "items": [
                {
                  "productId": "SKU-001",
                  "quantity": 2
                }
              ]
            }
            """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.customerId").value("customer"))
        .andExpect(jsonPath("$.status").value("PENDING"));
  }

  @Test
  void invalidOrderCreationRequestReturnsValidationError() throws Exception {
    String accessToken = TestAuthHelper.obtainAccessToken(mockMvc, objectMapper, "customer", "customer");

    mockMvc.perform(post("/api/orders")
        .header("Authorization", "Bearer " + accessToken)
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "customerId": "",
              "items": []
            }
            """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldSupportFullOrderLifecycleWithJwtAuthentication() throws Exception {
    String accessToken = TestAuthHelper.obtainAccessToken(mockMvc, objectMapper, "customer", "customer");

    String createdPayload = mockMvc.perform(post("/api/orders")
        .header("Authorization", "Bearer " + accessToken)
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
        .andExpect(jsonPath("$.status").value("PENDING"))
        .andReturn()
        .getResponse()
        .getContentAsString();

    String orderId = objectMapper.readTree(createdPayload).get("id").asText();

    mockMvc.perform(get("/api/orders/status/{orderId}", orderId)
        .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PENDING"));

    mockMvc.perform(post("/api/orders/{id}/payment", orderId)
        .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PAID"));

    mockMvc.perform(get("/api/orders/{id}", orderId)
        .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PAID"));
  }

  @Test
  void openApiEndpointIsAvailable() throws Exception {
    mockMvc.perform(get("/v3/api-docs"))
        .andExpect(status().isOk());
  }

  @Test
  @Sql(scripts = "/order-controller-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  void customersCannotReadOrdersTheyDoNotOwn() throws Exception {
    String customerToken = TestAuthHelper.obtainAccessToken(mockMvc, objectMapper, "customer", "customer");
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
        .andExpect(jsonPath("$.customerId").value("customer"))
        .andReturn()
        .getResponse()
        .getContentAsString();

    String orderId = objectMapper.readTree(createdPayload).get("id").asText();

    mockMvc.perform(get("/api/orders/status/{orderId}", orderId)
        .header("Authorization", "Bearer " + warehouseToken))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404));

    mockMvc.perform(get("/api/orders/{id}", orderId)
        .header("Authorization", "Bearer " + warehouseToken))
        .andExpect(status().isNotFound());

    mockMvc.perform(get("/api/orders")
        .param("customerId", "customer")
        .header("Authorization", "Bearer " + warehouseToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }
}
