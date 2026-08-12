package com.example.inventory.infrastructure.websocket;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;

import com.example.inventory.infrastructure.security.JwtAuthenticationConverter;

@Component
public class StompJwtChannelInterceptor implements ChannelInterceptor {

    private final JwtDecoder jwtDecoder;
    private final JwtAuthenticationConverter jwtAuthenticationConverter;

    public StompJwtChannelInterceptor(JwtDecoder jwtDecoder,
            JwtAuthenticationConverter jwtAuthenticationConverter) {
        this.jwtDecoder = jwtDecoder;
        this.jwtAuthenticationConverter = jwtAuthenticationConverter;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader == null) {
                authHeader = accessor.getFirstNativeHeader("authorization");
            }
            String token = null;
            if (authHeader != null) {
                if (authHeader.startsWith("Bearer ")) {
                    token = authHeader.substring(7);
                } else {
                    token = authHeader;
                }
            }
            // also accept token via sec-websocket-protocol header
            if (token == null) {
                String subProto = accessor.getFirstNativeHeader("sec-websocket-protocol");
                if (subProto != null) {
                    if (subProto.startsWith("Bearer ")) {
                        token = subProto.substring(7);
                    } else {
                        token = subProto;
                    }
                }
            }
            // also check handshake attributes populated by HandshakeInterceptor
            if (token == null && accessor.getSessionAttributes() != null) {
                Object a = accessor.getSessionAttributes().get("authToken");
                if (a instanceof String) {
                    token = (String) a;
                }
            }
            if (token != null) {
                Jwt jwt = jwtDecoder.decode(token);
                var authentication = jwtAuthenticationConverter.convert(jwt);
                if (authentication != null) {
                    accessor.setUser(authentication);
                }
            }
        }
        return message;
    }
}
