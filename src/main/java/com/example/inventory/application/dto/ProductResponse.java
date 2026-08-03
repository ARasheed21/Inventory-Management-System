package com.example.inventory.application.dto;

public record ProductResponse(String id, String name, String description, String price, String currency,
        int quantityInStock) {
}
