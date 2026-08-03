package com.example.inventory.application.handlers;

import java.util.List;

import org.springframework.stereotype.Component;

import com.example.inventory.application.dto.ProductResponse;
import com.example.inventory.application.queries.ListProductsQuery;
import com.example.inventory.domain.repositories.ProductRepository;

@Component
public class ListProductsQueryHandler {
    private final ProductRepository productRepository;

    public ListProductsQueryHandler(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<ProductResponse> handle(ListProductsQuery query) {
        return productRepository.findAll().stream()
                .map(product -> new ProductResponse(product.getId(), product.getName(), product.getDescription(),
                        product.getPrice().amount().toPlainString(), product.getPrice().currency(),
                        product.getQuantityInStock()))
                .toList();
    }
}
