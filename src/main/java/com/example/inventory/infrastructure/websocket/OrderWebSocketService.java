package com.example.inventory.infrastructure.websocket;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrderWebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    public OrderWebSocketService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publishOrderUpdate(String orderId, Object payload) {
        // Broadcast to topic; frontends can subscribe to /topic/orders or filter by
        // orderId
        messagingTemplate.convertAndSend("/topic/orders/" + orderId, payload);
    }

    public void publishToUser(String username, Object payload) {
        // Send a private message to the user destination
        messagingTemplate.convertAndSendToUser(username, "/queue/orders", payload);
    }
}
