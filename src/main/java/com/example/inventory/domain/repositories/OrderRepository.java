package com.example.inventory.domain.repositories;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.example.inventory.domain.entities.Order;

public interface OrderRepository {
    Order save(Order order);

    Optional<Order> findById(String id);

    List<Order> findByCustomerId(String customerId);

    List<Order> findAll();

    List<Order> cancelExpiredPendingOrders(Instant now);

    void delete(String id);
}
