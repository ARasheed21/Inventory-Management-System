package com.example.inventory.application.dto;

import java.util.List;

public record ProductPage(List<ProductResponse> content, long total, int page, int size) {
}
