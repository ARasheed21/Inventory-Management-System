package com.example.inventory.application.commands;

import com.example.inventory.application.dto.UpdateProductRequest;

public record UpdateProductCommand(String productId, UpdateProductRequest request) {
}
