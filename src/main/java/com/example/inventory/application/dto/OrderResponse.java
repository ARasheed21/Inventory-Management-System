package com.example.inventory.application.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderResponse(
        String id,
        String customerId,
        String status,
        BigDecimal totalAmount,
        String currency,
        Instant createdAt,
        Instant reservedUntil) {
}
