package com.example.inventory.infrastructure.persistence.jpa;

import com.example.inventory.domain.entities.Product;
import com.example.inventory.domain.valueobjects.Money;

public class ProductJpaMapper {

    public ProductJpaEntity toEntity(Product product) {
        return new ProductJpaEntity(product.getId(), product.getName(), product.getDescription(),
                product.getPrice().amount(), product.getPrice().currency(), product.getQuantityInStock(),
                product.getVersion());
    }

    public Product toDomain(ProductJpaEntity entity) {
        return new Product(entity.getExternalId(), entity.getName(), entity.getDescription(),
                Money.of(entity.getPrice().toPlainString(), entity.getCurrency()), entity.getQuantityInStock(),
                entity.getVersion());
    }
}
