package com.example.inventory.application.dto;

public record UpdateProductRequest(String name, String description, String price, String currency,
        int quantityInStock, String category) {
}
