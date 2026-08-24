package com.example.inventory.web.mapper;

import org.springframework.stereotype.Component;

import com.example.inventory.application.commands.PlaceOrderCommand;
import com.example.inventory.application.dto.CreateOrderItemRequest;
import com.example.inventory.web.dto.CreateOrderRequest;
import com.example.inventory.web.dto.OrderResponse;

@Component
public class OrderMapper {

    public PlaceOrderCommand toPlaceOrderCommand(CreateOrderRequest request) {
        return new PlaceOrderCommand(new com.example.inventory.application.dto.CreateOrderRequest(
                request.customerId(),
                request.items().stream()
                        .map(item -> new CreateOrderItemRequest(item.productId(), item.quantity()))
                        .toList()));
    }

    public OrderResponse toWebResponse(com.example.inventory.application.dto.OrderResponse response) {
        return new OrderResponse(
                response.id(),
                response.customerId(),
                response.status(),
                response.totalAmount(),
                response.currency(),
                response.createdAt(),
                response.reservedUntil(),
                reservationSecondsRemaining(response.status(), response.reservedUntil()));
    }

    private long reservationSecondsRemaining(String status, java.time.Instant reservedUntil) {
        if (!"PENDING".equals(status) || reservedUntil == null) {
            return 0;
        }
        long seconds = java.time.Duration.between(java.time.Instant.now(), reservedUntil).getSeconds();
        return Math.max(0, seconds);
    }
}
