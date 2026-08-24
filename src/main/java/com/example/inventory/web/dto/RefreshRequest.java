package com.example.inventory.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "RefreshRequest", description = "Body for POST /auth/refresh")
public record RefreshRequest(
        @Schema(description = "Refresh token previously returned by login/register") @NotBlank(message = "refreshToken is required") String refreshToken) {
}
