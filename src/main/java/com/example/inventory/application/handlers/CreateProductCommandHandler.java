package com.example.inventory.application.handlers;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.example.inventory.application.commands.CreateProductCommand;
import com.example.inventory.application.dto.CreateProductRequest;
import com.example.inventory.application.dto.ProductResponse;
import com.example.inventory.domain.entities.Product;
import com.example.inventory.domain.repositories.ProductRepository;
import com.example.inventory.domain.valueobjects.Money;

@Component
public class CreateProductCommandHandler {
    private final ProductRepository productRepository;

    public CreateProductCommandHandler(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public ProductResponse handle(CreateProductCommand command) {
        CreateProductRequest request = command.request();
        validateRequest(request);
        String id = UUID.randomUUID().toString();
        Product product = new Product(id, request.name(), request.description(),
                Money.of(request.price(), request.currency()), request.quantityInStock(), 0L, request.category());
        Product saved = productRepository.save(product);
        return new ProductResponse(saved.getId(), saved.getName(), saved.getDescription(),
                saved.getPrice().amount().toPlainString(), saved.getPrice().currency(), saved.getQuantityInStock(),
                saved.getCategory());
    }

    private void validateRequest(CreateProductRequest request) {
        if (request == null || request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException("Product name is required");
        }
        if (request.price() == null || request.price().isBlank()) {
            throw new IllegalArgumentException("Price is required");
        }
        if (request.currency() == null || request.currency().isBlank()) {
            throw new IllegalArgumentException("Currency is required");
        }
    }
}
