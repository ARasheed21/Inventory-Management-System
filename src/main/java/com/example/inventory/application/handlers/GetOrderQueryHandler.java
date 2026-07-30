package com.example.inventory.application.handlers;

import com.example.inventory.application.dto.OrderResponse;
import com.example.inventory.application.queries.GetOrderQuery;
import com.example.inventory.domain.Order;
import com.example.inventory.domain.OrderRepository;

public class GetOrderQueryHandler {
    private final OrderRepository orderRepository;

    public GetOrderQueryHandler(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public OrderResponse handle(GetOrderQuery query) {
        Order order = orderRepository.findById(query.orderId())
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + query.orderId()));
        return new OrderResponse(order.getId(), order.getCustomerId(), order.getStatus().name(),
                order.getItems().stream()
                        .map(item -> item.unitPrice().amount().multiply(java.math.BigDecimal.valueOf(item.quantity())))
                        .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add),
                "USD", order.getCreatedAt(), order.getReservedUntil());
    }
}
