package com.example.inventory.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AuthResponse", description = "JWT token pair returned by login and refresh flows")
public record AuthResponse(
        @Schema(description = "JWT access token; send as Authorization: Bearer <accessToken>") String accessToken,
        @Schema(description = "JWT refresh token; use with POST /auth/refresh") String refreshToken,
        @Schema(description = "Access token lifetime in seconds", example = "900") long expiresIn) {
}
