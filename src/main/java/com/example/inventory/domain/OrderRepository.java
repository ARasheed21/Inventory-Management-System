package com.example.inventory.domain;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    Order save(Order order);

    Optional<Order> findById(String id);

    List<Order> findByCustomerId(String customerId);

    List<Order> findAll();

    void delete(String id);
}
