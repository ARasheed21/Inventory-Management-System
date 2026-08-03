package com.example.inventory.infrastructure.jobs;

import java.time.Instant;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.inventory.domain.repositories.OrderRepository;

@Component
public class ReservationTimeoutJob {

    private final OrderRepository orderRepository;

    public ReservationTimeoutJob(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Scheduled(fixedDelayString = "${inventory.reservation-timeout.fixed-delay:60000}")
    @Transactional
    public void processExpiredReservations() {
        orderRepository.cancelExpiredPendingOrders(Instant.now());
    }
}
