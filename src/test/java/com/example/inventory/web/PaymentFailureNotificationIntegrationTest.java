package com.example.inventory.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.MediaType;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentFailureNotificationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Value("${local.server.port}")
    private int port;

    private StompSession subscribeToCustomerQueue(String accessToken, BlockingQueue<String> receivedMessages)
            throws Exception {
        WebSocketStompClient stompClient = new WebSocketStompClient(
                new SockJsClient(List.<Transport>of(new WebSocketTransport(new StandardWebSocketClient()))));
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + accessToken);

        StompSession session = stompClient.connectAsync(
                "ws://localhost:" + port + "/api/ws",
                new WebSocketHttpHeaders(),
                connectHeaders,
                new StompSessionHandlerAdapter() {
                }).get(5, TimeUnit.SECONDS);

        session.subscribe("/user/queue/orders", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                try {
                    receivedMessages.offer(objectMapper.writeValueAsString(payload), 5, TimeUnit.SECONDS);
                } catch (JsonProcessingException | InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        return session;
    }

    @Test
    @Sql(scripts = "/order-controller-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void successfulPaymentDoesNotEmitPaymentFailedNotification() throws Exception {
        String customerToken = TestAuthHelper.obtainAccessToken(mockMvc, objectMapper, "customer", "customer");

        BlockingQueue<String> receivedMessages = new ArrayBlockingQueue<>(1);
        StompSession session = subscribeToCustomerQueue(customerToken, receivedMessages);

        String payload = mockMvc.perform(post("/api/orders")
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
        String orderId = objectMapper.readTree(payload).get("id").asText();

        mockMvc.perform(post("/api/orders/{id}/payment", orderId)
                .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk());

        String rawMessage = receivedMessages.poll(3, TimeUnit.SECONDS);
        assertEquals(null, rawMessage, "No message should arrive after a successful payment");

        session.disconnect();
    }

    @Test
    @Sql(scripts = "/order-controller-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void payingUnknownOrderReturnsNotFound() throws Exception {
        String customerToken = TestAuthHelper.obtainAccessToken(mockMvc, objectMapper, "customer", "customer");

        mockMvc.perform(post("/api/orders/{id}/payment", "no-such-order")
                .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @Sql(scripts = "/expired-reservation-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void payingExpiredOrderNotifiesCustomerOverWebSocketAndReturnsConflict() throws Exception {
        String customerToken = TestAuthHelper.obtainAccessToken(mockMvc, objectMapper, "customer", "customer");

        BlockingQueue<String> receivedMessages = new ArrayBlockingQueue<>(1);
        StompSession session = subscribeToCustomerQueue(customerToken, receivedMessages);

        mockMvc.perform(post("/api/orders/{id}/payment", "ord-expired-1")
                .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isConflict());

        String rawMessage = receivedMessages.poll(10, TimeUnit.SECONDS);
        assertNotNull(rawMessage, "Expected a PAYMENT_FAILED message on /user/queue/orders");

        JsonNode payload = objectMapper.readTree(rawMessage);
        assertEquals("ord-expired-1", payload.get("orderId").asText());
        assertEquals("PAYMENT_FAILED", payload.get("status").asText());
        assertNotNull(payload.get("reason").asText());

        session.disconnect();
    }
}
