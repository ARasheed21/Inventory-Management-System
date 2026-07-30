package com.example.inventory.application.handlers;

import com.example.inventory.application.dto.OrderResponse;
import com.example.inventory.application.queries.ListOrdersQuery;
import com.example.inventory.domain.entities.Order;
import com.example.inventory.domain.repositories.OrderRepository;

import java.util.List;

public class ListOrdersQueryHandler {
    private final OrderRepository orderRepository;

    public ListOrdersQueryHandler(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public List<OrderResponse> handle(ListOrdersQuery query) {
        List<Order> orders = orderRepository.findByCustomerId(query.customerId());
        return orders.stream()
                .filter(order -> query.status() == null || order.getStatus().name().equalsIgnoreCase(query.status()))
                .map(order -> new OrderResponse(order.getId(), order.getCustomerId(), order.getStatus().name(), order
                        .getItems().stream()
                        .map(item -> item.unitPrice().amount().multiply(java.math.BigDecimal.valueOf(item.quantity())))
                        .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add), "USD", order.getCreatedAt(),
                        order.getReservedUntil()))
                .toList();
    }
}
