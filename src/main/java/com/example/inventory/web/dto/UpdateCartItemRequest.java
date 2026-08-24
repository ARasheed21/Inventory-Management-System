package com.example.inventory.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;

@Schema(name = "UpdateCartItemRequest", description = "Payload used to update a cart item. productId is optional; when omitted the item keeps its current product.")
public record UpdateCartItemRequest(
        @Schema(description = "New product id (optional - omit to only change quantity)", example = "product-1") String productId,
        @Schema(description = "Quantity of the product in the cart", example = "2") @Min(value = 1, message = "quantity must be at least 1") int quantity) {
}
