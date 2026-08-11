package com.example.inventory.web.controllers;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.inventory.application.commands.CreateProductCommand;
import com.example.inventory.application.commands.UpdateProductCommand;
import com.example.inventory.application.dto.InventoryItemResponse;
import com.example.inventory.application.dto.ProductResponse;
import com.example.inventory.application.handlers.CreateProductCommandHandler;
import com.example.inventory.application.handlers.GetInventoryQueryHandler;
import com.example.inventory.application.handlers.ListProductsQueryHandler;
import com.example.inventory.application.handlers.UpdateProductCommandHandler;
import com.example.inventory.application.queries.GetInventoryQuery;
import com.example.inventory.application.queries.ListProductsQuery;
import com.example.inventory.web.dto.CreateProductWebRequest;
import com.example.inventory.web.dto.InventoryResponse;
import com.example.inventory.web.dto.UpdateProductWebRequest;
import com.example.inventory.web.mapper.ProductMapper;

@RestController
@RequestMapping({ "/api", "" })
@Tag(name = "Inventory", description = "Inventory query and management endpoints")
public class InventoryController {

    private final GetInventoryQueryHandler getInventoryQueryHandler;
    private final CreateProductCommandHandler createProductCommandHandler;
    private final UpdateProductCommandHandler updateProductCommandHandler;
    private final ListProductsQueryHandler listProductsQueryHandler;
    private final ProductMapper productMapper;

    public InventoryController(GetInventoryQueryHandler getInventoryQueryHandler,
            CreateProductCommandHandler createProductCommandHandler,
            UpdateProductCommandHandler updateProductCommandHandler,
            ListProductsQueryHandler listProductsQueryHandler,
            ProductMapper productMapper) {
        this.getInventoryQueryHandler = getInventoryQueryHandler;
        this.createProductCommandHandler = createProductCommandHandler;
        this.updateProductCommandHandler = updateProductCommandHandler;
        this.listProductsQueryHandler = listProductsQueryHandler;
        this.productMapper = productMapper;
    }

    @GetMapping("/inventory")
    @Operation(summary = "List inventory", description = "Returns the first available inventory snapshot from the application query handler.")
    @ApiResponse(responseCode = "200", description = "Inventory returned")
    public ResponseEntity<InventoryResponse> getInventory() {
        InventoryItemResponse item = getInventoryQueryHandler.handle(new GetInventoryQuery());
        return ResponseEntity.ok(new InventoryResponse(item.productId(), item.name(), item.quantityInStock()));
    }

    @GetMapping("/products")
    @Operation(summary = "List products", description = "Returns the browsable product catalog for customer and admin use.")
    @ApiResponse(responseCode = "200", description = "Products returned")
    public ResponseEntity<List<com.example.inventory.web.dto.ProductResponse>> listProducts() {
        List<ProductResponse> responses = listProductsQueryHandler.handle(new ListProductsQuery());
        return ResponseEntity.ok(responses.stream().map(productMapper::toWebResponse).toList());
    }

    @PostMapping("/inventory/products")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create product entry", description = "Creates a product record and stores its initial stock level.")
    @ApiResponse(responseCode = "201", description = "Product created")
    public ResponseEntity<com.example.inventory.web.dto.ProductResponse> createProduct(
            @Valid @RequestBody CreateProductWebRequest request) {
        CreateProductCommand command = productMapper.toCreateCommand(request);
        ProductResponse response = createProductCommandHandler.handle(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(productMapper.toWebResponse(response));
    }

    @PutMapping("/inventory/products/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update product entry", description = "Updates product details and quantity.")
    @ApiResponse(responseCode = "200", description = "Product updated")
    public ResponseEntity<com.example.inventory.web.dto.ProductResponse> updateProduct(@PathVariable String id,
            @Valid @RequestBody UpdateProductWebRequest request) {
        UpdateProductCommand command = productMapper.toUpdateCommand(id, request);
        ProductResponse response = updateProductCommandHandler.handle(command);
        return ResponseEntity.ok(productMapper.toWebResponse(response));
    }
}
