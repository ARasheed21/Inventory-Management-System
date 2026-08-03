package com.example.inventory.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "InventoryResponse", description = "Inventory payload returned by the REST API")
public record InventoryResponse(
        @Schema(description = "Product identifier", example = "SKU-001")
        String productId,

        @Schema(description = "Product name", example = "Widget")
        String name,

        @Schema(description = "Current stock quantity", example = "25")
        int quantityInStock) {
}
