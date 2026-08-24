package com.example.inventory.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "LoginRequest", description = "Credentials for POST /auth/login")
public record LoginRequest(
        @Schema(description = "Username of the account", example = "customer") @NotBlank(message = "username is required") String username,
        @Schema(description = "Account password", example = "customer") @NotBlank(message = "password is required") String password) {
}
