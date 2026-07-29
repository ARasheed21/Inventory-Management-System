package com.example.inventory.domain;

public record SKU(String value) {

    public SKU {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("SKU is required");
        }
    }

    public static SKU of(String value) {
        return new SKU(value);
    }
}
