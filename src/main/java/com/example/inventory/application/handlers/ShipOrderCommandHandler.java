package com.example.inventory.application.handlers;

import com.example.inventory.application.ResourceNotFoundException;
import com.example.inventory.application.commands.ShipOrderCommand;
import com.example.inventory.application.dto.OrderResponse;
import com.example.inventory.domain.entities.Order;
import com.example.inventory.domain.repositories.OrderRepository;
import org.springframework.stereotype.Component;

@Component
public class ShipOrderCommandHandler {
    private final OrderRepository orderRepository;

    public ShipOrderCommandHandler(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public OrderResponse handle(ShipOrderCommand command) {
        Order order = orderRepository.findById(command.orderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + command.orderId()));
        order.ship();
        orderRepository.save(order);
        return new OrderResponse(order.getId(), order.getCustomerId(), order.getStatus().name(),
                order.getItems().stream()
                        .map(item -> item.unitPrice().amount().multiply(java.math.BigDecimal.valueOf(item.quantity())))
                        .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add),
                "USD", order.getCreatedAt(), order.getReservedUntil());
    }
}
