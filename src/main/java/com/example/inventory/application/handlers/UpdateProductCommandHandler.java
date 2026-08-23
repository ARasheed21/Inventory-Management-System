package com.example.inventory.application.handlers;

import org.springframework.stereotype.Component;

import com.example.inventory.application.commands.UpdateProductCommand;
import com.example.inventory.application.dto.ProductResponse;
import com.example.inventory.application.dto.UpdateProductRequest;
import com.example.inventory.domain.entities.Product;
import com.example.inventory.domain.repositories.ProductRepository;
import com.example.inventory.domain.valueobjects.Money;

@Component
public class UpdateProductCommandHandler {
    private final ProductRepository productRepository;

    public UpdateProductCommandHandler(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public ProductResponse handle(UpdateProductCommand command) {
        UpdateProductRequest request = command.request();
        validateRequest(request);
        Product existing = productRepository.findById(command.productId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + command.productId()));

        Product updated = new Product(existing.getId(), request.name(), request.description(),
                Money.of(request.price(), request.currency()), request.quantityInStock(), existing.getVersion(),
                request.category() != null ? request.category() : existing.getCategory());
        Product saved = productRepository.save(updated);
        return new ProductResponse(saved.getId(), saved.getName(), saved.getDescription(),
                saved.getPrice().amount().toPlainString(), saved.getPrice().currency(), saved.getQuantityInStock(),
                saved.getCategory());
    }

    private void validateRequest(UpdateProductRequest request) {
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
