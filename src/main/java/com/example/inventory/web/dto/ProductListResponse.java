package com.example.inventory.web.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ProductListResponse", description = "Paginated product catalog page")
public record ProductListResponse(
                @Schema(description = "Products on this page") List<ProductResponse> content,
                @Schema(description = "Total number of matching products", example = "42") long total,
                @Schema(description = "Current page number (0-based)", example = "0") int page,
                @Schema(description = "Page size", example = "20") int size) {
}
