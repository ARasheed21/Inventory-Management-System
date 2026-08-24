package com.example.inventory.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(name = "RegisterRequest", description = "Payload for POST /auth/register. Creates an account with ROLE_CUSTOMER.")
public record RegisterRequest(
        @Schema(description = "Unique username; becomes the order customerId for this user", example = "alice")
        @NotBlank String username,
        @Schema(description = "Unique email address", example = "alice@example.com")
        @NotBlank @Email String email,
        @Schema(description = "Password: minimum 8 characters, must contain at least one letter and one digit",
                example = "s3cret-pass", minLength = 8)
        @NotBlank
        @Size(min = 8, message = "password must be at least 8 characters")
        @Pattern(regexp = ".*[A-Za-z].*", message = "password must contain a letter")
        @Pattern(regexp = ".*\\d.*", message = "password must contain a digit")
        String password) {
}
