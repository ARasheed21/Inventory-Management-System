package com.example.inventory.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ProductResponse", description = "Product payload returned by the REST API")
public record ProductResponse(
        @Schema(description = "Product identifier", example = "SKU-001") String id,
        @Schema(description = "Product name", example = "Desk Lamp") String name,
        @Schema(description = "Product description", example = "Compact desk lamp") String description,
        @Schema(description = "Unit price", example = "45.00") String price,
        @Schema(description = "Currency code", example = "USD") String currency,
        @Schema(description = "Inventory quantity on hand", example = "25") int quantityInStock,
        @Schema(description = "Product category", example = "lighting", nullable = true) String category) {
}
