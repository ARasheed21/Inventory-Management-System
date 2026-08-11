package com.example.inventory.domain.entities;

public record CartItem(String id, String customerId, String productId, int quantity) {

    public CartItem {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Cart item id is required");
        }
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("Customer id is required");
        }
        if (productId == null || productId.isBlank()) {
            throw new IllegalArgumentException("Product id is required");
        }
        if (quantity < 1) {
            throw new IllegalArgumentException("Quantity must be at least 1");
        }
    }

    public CartItem withQuantity(int quantity) {
        return new CartItem(id, customerId, productId, quantity);
    }

    @Override
    public String toString() {
        return "CartItem[id=" + id + ", customerId=" + customerId + ", productId=" + productId + ", quantity="
                + quantity + "]";
    }
}
