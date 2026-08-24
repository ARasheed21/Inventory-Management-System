package com.example.inventory.application.dto;

public record ReservedInventoryItem(
        String productId,
        String name,
        int quantityInStock,
        int quantityReserved,
        int quantityAvailable) {
}
