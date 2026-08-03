package com.example.inventory.web.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

@Schema(name = "CreateOrderRequest", description = "Payload used to create a new order")
public record CreateOrderRequest(
        @Schema(description = "Customer identifier", example = "customer-001") @NotBlank(message = "customerId is required") String customerId,

        @Schema(description = "Order items to reserve") @NotEmpty(message = "items must contain at least one item") @Valid List<CreateOrderItemRequest> items) {
}
