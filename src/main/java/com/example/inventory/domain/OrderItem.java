package com.example.inventory.domain;

public record OrderItem(String productId, int quantity, Money unitPrice, SKU sku) {

    public OrderItem {
        if (productId == null || productId.isBlank()) {
            throw new IllegalArgumentException("Product id is required");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (unitPrice == null) {
            throw new IllegalArgumentException("Unit price is required");
        }
        if (sku == null) {
            throw new IllegalArgumentException("SKU is required");
        }
    }
}
