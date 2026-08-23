package com.example.inventory.domain.entities;

import com.example.inventory.domain.valueobjects.Money;

public class Product {
    private final String id;
    private final String name;
    private final String description;
    private final Money price;
    private final int quantityInStock;
    private final long version;
    private final String category;

    public Product(String id, String name, String description, Money price, int quantityInStock, long version) {
        this(id, name, description, price, quantityInStock, version, null);
    }

    public Product(String id, String name, String description, Money price, int quantityInStock, long version,
            String category) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Product id is required");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Product name is required");
        }
        if (price == null) {
            throw new IllegalArgumentException("Price is required");
        }
        if (quantityInStock < 0) {
            throw new IllegalArgumentException("Quantity in stock cannot be negative");
        }
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.quantityInStock = quantityInStock;
        this.version = version;
        this.category = category;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Money getPrice() {
        return price;
    }

    public int getQuantityInStock() {
        return quantityInStock;
    }

    public long getVersion() {
        return version;
    }

    public String getCategory() {
        return category;
    }
}
