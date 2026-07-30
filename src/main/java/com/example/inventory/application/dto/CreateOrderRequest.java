package com.example.inventory.application.dto;

import java.util.List;

public record CreateOrderRequest(String customerId, List<CreateOrderItemRequest> items) {
}
