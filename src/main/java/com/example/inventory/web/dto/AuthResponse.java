package com.example.inventory.web.dto;

public record AuthResponse(String accessToken, String refreshToken, long expiresIn) {
}
