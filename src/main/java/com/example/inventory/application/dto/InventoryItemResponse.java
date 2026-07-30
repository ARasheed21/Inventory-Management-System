package com.example.inventory.application.dto;

public record InventoryItemResponse(String productId, String name, int quantityInStock) {
}
