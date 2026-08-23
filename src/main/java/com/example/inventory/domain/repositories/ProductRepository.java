package com.example.inventory.domain.repositories;

import java.util.List;
import java.util.Optional;

import com.example.inventory.domain.entities.Product;

public interface ProductRepository {
    Optional<Product> findById(String id);

    List<Product> findAll();

    SearchResult search(String searchTerm, String category, int page, int size);

    Product save(Product product);

    record SearchResult(List<Product> products, long total, int page, int size) {
    }
}
