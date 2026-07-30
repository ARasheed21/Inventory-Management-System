package com.example.inventory.application.commands;

import com.example.inventory.application.dto.CreateOrderRequest;

public record PlaceOrderCommand(CreateOrderRequest request) {
}
