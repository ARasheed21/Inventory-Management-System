package com.example.inventory.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(name = "UpdateProductRequest", description = "Payload used to update an existing product entry")
public record UpdateProductWebRequest(
                @Schema(description = "Product name", example = "Desk Lamp") @NotBlank(message = "name is required") String name,
                @Schema(description = "Product description", example = "Compact desk lamp") String description,
                @Schema(description = "Product price", example = "45.00") @NotBlank(message = "price is required") String price,
                @Schema(description = "Currency code", example = "USD") @NotBlank(message = "currency is required") String currency,
                @Schema(description = "Inventory quantity on hand", example = "25") @NotNull(message = "quantityInStock is required") @Min(value = 0, message = "quantityInStock must be non-negative") int quantityInStock,
                @Schema(description = "Product category", example = "lighting") String category) {
}
