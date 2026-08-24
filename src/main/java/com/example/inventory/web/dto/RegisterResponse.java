package com.example.inventory.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "RegisterResponse", description = "Returned after successful registration; the account is created and tokens are issued immediately")
public record RegisterResponse(
        @Schema(description = "Username of the created account", example = "alice") String username,
        @Schema(description = "JWT access token for Authorization: Bearer headers") String accessToken,
        @Schema(description = "JWT refresh token for POST /auth/refresh") String refreshToken,
        @Schema(description = "Access token lifetime in seconds", example = "900") long expiresIn) {
}
