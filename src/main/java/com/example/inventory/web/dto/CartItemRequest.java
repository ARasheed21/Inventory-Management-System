package com.example.inventory.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "CartItemRequest", description = "Payload used to create or update a cart item")
public record CartItemRequest(
        @Schema(description = "Identifier of the product to add to the cart", example = "product-1") @NotBlank(message = "productId is required") String productId,
        @Schema(description = "Quantity of the product in the cart", example = "2") @Min(value = 1, message = "quantity must be at least 1") int quantity) {
}
