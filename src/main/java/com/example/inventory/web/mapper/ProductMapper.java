package com.example.inventory.web.mapper;

import org.springframework.stereotype.Component;

import com.example.inventory.application.commands.CreateProductCommand;
import com.example.inventory.application.commands.UpdateProductCommand;
import com.example.inventory.application.dto.ProductResponse;

@Component
public class ProductMapper {

    public CreateProductCommand toCreateCommand(com.example.inventory.web.dto.CreateProductWebRequest request) {
        return new CreateProductCommand(new com.example.inventory.application.dto.CreateProductRequest(
                request.name(),
                request.description(),
                request.price(),
                request.currency(),
                request.quantityInStock(),
                request.category()));
    }

    public UpdateProductCommand toUpdateCommand(String id,
            com.example.inventory.web.dto.UpdateProductWebRequest request) {
        return new UpdateProductCommand(id,
                new com.example.inventory.application.dto.UpdateProductRequest(
                        request.name(),
                        request.description(),
                        request.price(),
                        request.currency(),
                        request.quantityInStock(),
                        request.category()));
    }

    public com.example.inventory.web.dto.ProductResponse toWebResponse(ProductResponse response) {
        return new com.example.inventory.web.dto.ProductResponse(
                response.id(),
                response.name(),
                response.description(),
                response.price(),
                response.currency(),
                response.quantityInStock(),
                response.category());
    }
}
