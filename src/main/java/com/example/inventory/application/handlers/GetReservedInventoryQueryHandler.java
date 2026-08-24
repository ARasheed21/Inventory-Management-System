package com.example.inventory.application.handlers;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.inventory.application.dto.ReservedInventoryItem;
import com.example.inventory.domain.repositories.OrderRepository;
import com.example.inventory.domain.repositories.ProductRepository;
import com.example.inventory.domain.entities.Product;

@Component
public class GetReservedInventoryQueryHandler {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    public GetReservedInventoryQueryHandler(ProductRepository productRepository, OrderRepository orderRepository) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public List<ReservedInventoryItem> handle() {
        var reservedByProduct = orderRepository.findReservedQuantitiesByProduct();
        return productRepository.findAll().stream()
                .map(product -> toItem(product, reservedByProduct.getOrDefault(product.getId(), 0)))
                .toList();
    }

    private ReservedInventoryItem toItem(Product product, int reserved) {
        return new ReservedInventoryItem(
                product.getId(),
                product.getName(),
                product.getQuantityInStock(),
                reserved,
                Math.max(0, product.getQuantityInStock() - reserved));
    }
}
