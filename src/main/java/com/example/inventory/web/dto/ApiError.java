package com.example.inventory.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ApiError", description = "Standard error body returned by every non-2xx response")
public record ApiError(
        @Schema(description = "ISO-8601 timestamp of when the error occurred") String timestamp,
        @Schema(description = "HTTP status code", example = "404") int status,
        @Schema(description = "Short error category", example = "Not found") String error,
        @Schema(description = "Human-readable detail message") String message,
        @Schema(description = "Request path that produced the error", example = "/api/orders/unknown") String path) {
}
