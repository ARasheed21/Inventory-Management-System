package com.example.inventory.domain.repositories;

import java.util.List;
import java.util.Optional;

import com.example.inventory.domain.entities.CartItem;

public interface CartRepository {
    List<CartItem> findByCustomerId(String customerId);

    Optional<CartItem> findById(String itemId);

    Optional<CartItem> findByCustomerIdAndProductId(String customerId, String productId);

    CartItem save(CartItem cartItem);

    void deleteById(String itemId);
}
