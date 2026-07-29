package com.example.inventory.domain;

import java.util.Optional;

public interface OrderRepository {
    Order save(Order order);

    Optional<Order> findById(String id);

    void delete(String id);
}
