package com.example.inventory.application.queries;

public record ListProductsQuery(String searchTerm, String category, int page, int size) {
}
