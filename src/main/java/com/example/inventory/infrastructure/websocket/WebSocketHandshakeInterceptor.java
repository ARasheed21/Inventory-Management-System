package com.example.inventory.infrastructure.websocket;

import java.util.Map;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
            Map<String, Object> attributes) throws Exception {
        // Extract Sec-WebSocket-Protocol header if present (some clients send the JWT
        // here)
        String proto = request.getHeaders().getFirst("Sec-WebSocket-Protocol");
        if (proto != null && !proto.isBlank()) {
            // common formats: "Bearer <token>" or raw token
            String token = proto;
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            attributes.put("authToken", token);
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
            Exception exception) {
        // no-op
    }
}
