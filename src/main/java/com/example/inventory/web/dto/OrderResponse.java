package com.example.inventory.web.dto;

import java.math.BigDecimal;
import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "OrderResponse", description = "Order payload returned by the REST API")
public record OrderResponse(
        @Schema(description = "Order identifier", example = "ord-123") String id,

        @Schema(description = "Customer identifier", example = "customer-001") String customerId,

        @Schema(description = "Order lifecycle status", example = "PENDING") String status,

        @Schema(description = "Order total amount", example = "25.00") BigDecimal totalAmount,

        @Schema(description = "Currency code", example = "USD") String currency,

        @Schema(description = "Order creation timestamp") Instant createdAt,

        @Schema(description = "Reservation expiry timestamp") Instant reservedUntil) {
}
