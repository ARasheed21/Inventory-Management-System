package com.example.inventory.application.handlers;

import org.springframework.stereotype.Component;

import com.example.inventory.application.dto.ProductResponse;
import com.example.inventory.application.queries.GetProductQuery;
import com.example.inventory.domain.repositories.ProductRepository;
import com.example.inventory.application.ResourceNotFoundException;

@Component
public class GetProductQueryHandler {

    private final ProductRepository productRepository;

    public GetProductQueryHandler(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public ProductResponse handle(GetProductQuery query) {
        return productRepository.findById(query.productId())
                .map(product -> new ProductResponse(product.getId(), product.getName(), product.getDescription(),
                        product.getPrice().amount().toPlainString(), product.getPrice().currency(),
                        product.getQuantityInStock(), product.getCategory()))
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + query.productId()));
    }
}
