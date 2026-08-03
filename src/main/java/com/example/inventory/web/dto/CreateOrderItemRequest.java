package com.example.inventory.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "CreateOrderItemRequest", description = "Order item payload for a product reservation")
public record CreateOrderItemRequest(
        @Schema(description = "Identifier of the product to reserve", example = "SKU-001") @NotBlank(message = "productId is required") String productId,

        @Schema(description = "Requested item quantity", example = "2") @Min(value = 1, message = "quantity must be at least 1") int quantity) {
}
