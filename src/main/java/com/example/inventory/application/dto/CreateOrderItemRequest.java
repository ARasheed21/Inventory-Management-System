package com.example.inventory.application.dto;

public record CreateOrderItemRequest(String productId, int quantity) {
}
