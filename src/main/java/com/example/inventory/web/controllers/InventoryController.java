package com.example.inventory.web.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.inventory.application.dto.InventoryItemResponse;
import com.example.inventory.application.handlers.GetInventoryQueryHandler;
import com.example.inventory.application.queries.GetInventoryQuery;
import com.example.inventory.web.dto.InventoryResponse;

@RestController
@RequestMapping("/api")
@Tag(name = "Inventory", description = "Inventory query and management endpoints")
public class InventoryController {

    private final GetInventoryQueryHandler getInventoryQueryHandler;

    public InventoryController(GetInventoryQueryHandler getInventoryQueryHandler) {
        this.getInventoryQueryHandler = getInventoryQueryHandler;
    }

    @GetMapping("/inventory")
    @Operation(summary = "List inventory", description = "Returns the first available inventory snapshot from the application query handler.")
    @ApiResponse(responseCode = "200", description = "Inventory returned")
    public ResponseEntity<InventoryResponse> getInventory() {
        InventoryItemResponse item = getInventoryQueryHandler.handle(new GetInventoryQuery());
        return ResponseEntity.ok(new InventoryResponse(item.productId(), item.name(), item.quantityInStock()));
    }

    @PostMapping("/inventory/products")
    @Operation(summary = "Create product entry", description = "Inventory product creation endpoint placeholder for the web contract.")
    @ApiResponse(responseCode = "501", description = "Not implemented in the current application layer")
    public ResponseEntity<Void> createProduct(@RequestBody Object request) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @PutMapping("/inventory/products/{id}")
    @Operation(summary = "Update product entry", description = "Inventory product update endpoint placeholder for the web contract.")
    @ApiResponse(responseCode = "501", description = "Not implemented in the current application layer")
    public ResponseEntity<Void> updateProduct(@PathVariable String id, @RequestBody Object request) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}
