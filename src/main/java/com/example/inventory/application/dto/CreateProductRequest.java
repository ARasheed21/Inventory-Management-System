package com.example.inventory.application.dto;

public record CreateProductRequest(String name, String description, String price, String currency,
        int quantityInStock) {
}
