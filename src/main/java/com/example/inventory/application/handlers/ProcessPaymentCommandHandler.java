package com.example.inventory.application.handlers;

import java.time.Instant;

import org.springframework.stereotype.Component;

import com.example.inventory.application.commands.ProcessPaymentCommand;
import com.example.inventory.application.dto.OrderResponse;
import com.example.inventory.application.ports.PaymentFailureNotifier;
import com.example.inventory.application.ResourceNotFoundException;
import com.example.inventory.domain.entities.Order;
import com.example.inventory.domain.repositories.OrderRepository;

@Component
public class ProcessPaymentCommandHandler {
    private final OrderRepository orderRepository;
    private final PaymentFailureNotifier paymentFailureNotifier;

    public ProcessPaymentCommandHandler(OrderRepository orderRepository,
            PaymentFailureNotifier paymentFailureNotifier) {
        this.orderRepository = orderRepository;
        this.paymentFailureNotifier = paymentFailureNotifier;
    }

    public OrderResponse handle(ProcessPaymentCommand command) {
        Order order = orderRepository.findById(command.orderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + command.orderId()));
        try {
            ensurePayable(order);
            order.pay();
        } catch (IllegalStateException ex) {
            paymentFailureNotifier.notifyPaymentFailed(order.getId(), order.getCustomerId(), ex.getMessage());
            throw ex;
        }
        orderRepository.save(order);
        return toResponse(order);
    }

    private void ensurePayable(Order order) {
        if (order.isReservationExpired(Instant.now())) {
            throw new IllegalStateException("Reservation expired for order: " + order.getId());
        }
    }

    private OrderResponse toResponse(Order order) {
        return new OrderResponse(order.getId(), order.getCustomerId(), order.getStatus().name(),
                order.getItems().stream()
                        .map(item -> item.unitPrice().amount().multiply(java.math.BigDecimal.valueOf(item.quantity())))
                        .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add),
                "USD", order.getCreatedAt(), order.getReservedUntil());
    }
}
