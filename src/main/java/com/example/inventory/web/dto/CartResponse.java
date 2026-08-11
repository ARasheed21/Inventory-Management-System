package com.example.inventory.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CartResponse", description = "Cart item returned to the client")
public record CartResponse(String id, String customerId, String productId, int quantity) {
}
