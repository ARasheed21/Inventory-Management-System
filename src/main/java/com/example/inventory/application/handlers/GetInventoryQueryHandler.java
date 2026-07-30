package com.example.inventory.application.handlers;

import com.example.inventory.application.dto.InventoryItemResponse;
import com.example.inventory.application.queries.GetInventoryQuery;
import com.example.inventory.domain.entities.Product;
import com.example.inventory.domain.repositories.ProductRepository;

import java.util.List;

public class GetInventoryQueryHandler {
    private final ProductRepository productRepository;

    public GetInventoryQueryHandler(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public InventoryItemResponse handle(GetInventoryQuery query) {
        List<Product> products = productRepository.findAll();
        if (products.isEmpty()) {
            throw new IllegalArgumentException("No products available");
        }
        Product product = products.get(0);
        return new InventoryItemResponse(product.getId(), product.getName(), product.getQuantityInStock());
    }
}
