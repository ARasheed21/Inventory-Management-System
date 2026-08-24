package com.example.inventory.infrastructure.jobs;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.inventory.domain.entities.Order;
import com.example.inventory.domain.repositories.OrderRepository;
import com.example.inventory.infrastructure.websocket.OrderWebSocketService;

@Component
public class ReservationTimeoutJob {

    private final OrderRepository orderRepository;
    private final OrderWebSocketService orderWebSocketService;

    public ReservationTimeoutJob(OrderRepository orderRepository, OrderWebSocketService orderWebSocketService) {
        this.orderRepository = orderRepository;
        this.orderWebSocketService = orderWebSocketService;
    }

    @Scheduled(fixedDelayString = "${inventory.reservation-timeout.fixed-delay:60000}")
    @Transactional
    public void processExpiredReservations() {
        List<Order> expiredOrders = orderRepository.cancelExpiredPendingOrders(Instant.now());
        for (Order order : expiredOrders) {
            orderWebSocketService.publishToUser(order.getCustomerId(),
                    Map.of("orderId", order.getId(), "status", "RESERVATION_EXPIRED"));
        }
    }
}
