package com.example.inventory.domain.repositories;

import java.util.List;
import java.util.Optional;

import com.example.inventory.domain.entities.Product;

public interface ProductRepository {
    Optional<Product> findById(String id);

    List<Product> findAll();

    Product save(Product product);
}
