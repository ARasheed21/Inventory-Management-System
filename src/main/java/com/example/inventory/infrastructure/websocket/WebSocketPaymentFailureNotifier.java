package com.example.inventory.infrastructure.websocket;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.example.inventory.application.ports.PaymentFailureNotifier;

@Component
public class WebSocketPaymentFailureNotifier implements PaymentFailureNotifier {

    private final OrderWebSocketService orderWebSocketService;

    public WebSocketPaymentFailureNotifier(OrderWebSocketService orderWebSocketService) {
        this.orderWebSocketService = orderWebSocketService;
    }

    @Override
    public void notifyPaymentFailed(String orderId, String customerId, String reason) {
        orderWebSocketService.publishToUser(customerId,
                Map.of("orderId", orderId, "status", "PAYMENT_FAILED", "reason", reason));
    }
}
