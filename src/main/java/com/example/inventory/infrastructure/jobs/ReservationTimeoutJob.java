package com.example.inventory.infrastructure.jobs;

import java.time.Instant;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.inventory.application.commands.CancelOrderCommand;
import com.example.inventory.application.handlers.CancelOrderCommandHandler;
import com.example.inventory.domain.entities.Order;
import com.example.inventory.domain.repositories.OrderRepository;

@Component
public class ReservationTimeoutJob {

    private final OrderRepository orderRepository;
    private final CancelOrderCommandHandler cancelOrderCommandHandler;

    public ReservationTimeoutJob(OrderRepository orderRepository, CancelOrderCommandHandler cancelOrderCommandHandler) {
        this.orderRepository = orderRepository;
        this.cancelOrderCommandHandler = cancelOrderCommandHandler;
    }

    @Scheduled(fixedDelayString = "${inventory.reservation-timeout.fixed-delay:60000}")
    @Transactional
    public void processExpiredReservations() {
        List<Order> pendingOrders = orderRepository.findAll().stream()
                .filter(order -> order.getStatus() == com.example.inventory.domain.valueobjects.OrderStatus.PENDING)
                .filter(order -> order.isReservationExpired(Instant.now()))
                .toList();

        for (Order order : pendingOrders) {
            cancelOrderCommandHandler.handle(new CancelOrderCommand(order.getId()));
        }
    }
}
