package com.example.inventory.web.controllers;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import com.example.inventory.application.handlers.GetProductQueryHandler;
import com.example.inventory.application.handlers.ListProductsQueryHandler;
import com.example.inventory.application.handlers.UpdateProductCommandHandler;
import com.example.inventory.application.queries.GetInventoryQuery;
import com.example.inventory.application.queries.GetProductQuery;
import com.example.inventory.application.queries.ListProductsQuery;
import com.example.inventory.web.dto.CreateProductWebRequest;
import com.example.inventory.web.dto.InventoryResponse;
import com.example.inventory.web.dto.UpdateProductWebRequest;
import com.example.inventory.web.mapper.ProductMapper;

@RestController
@RequestMapping({ "/api", "" })
@Tag(name = "Inventory", description = "Inventory query and management endpoints")
public class InventoryController {

    private final GetProductQueryHandler getProductQueryHandler;
    private final GetInventoryQueryHandler getInventoryQueryHandler;
    private final CreateProductCommandHandler createProductCommandHandler;
    private final UpdateProductCommandHandler updateProductCommandHandler;
    private final ListProductsQueryHandler listProductsQueryHandler;
    private final ProductMapper productMapper;

    public InventoryController(GetProductQueryHandler getProductQueryHandler,
            GetInventoryQueryHandler getInventoryQueryHandler,
            CreateProductCommandHandler createProductCommandHandler,
            UpdateProductCommandHandler updateProductCommandHandler,
            ListProductsQueryHandler listProductsQueryHandler,
            ProductMapper productMapper) {
        this.getProductQueryHandler = getProductQueryHandler;
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
    @Operation(summary = "List products", description = "Returns a paginated, searchable product catalog page.")
    @ApiResponse(responseCode = "200", description = "Products returned")
    public ResponseEntity<com.example.inventory.web.dto.ProductListResponse> listProducts(
            @org.springframework.web.bind.annotation.RequestParam(required = false)
            @Parameter(description = "Free-text search across name and description") String search,
            @org.springframework.web.bind.annotation.RequestParam(required = false)
            @Parameter(description = "Exact category filter (case-insensitive)") String category,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0")
            @Parameter(description = "Page number (0-based)") int page,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "20")
            @Parameter(description = "Page size (1-100)") int size) {
        var result = listProductsQueryHandler
                .handle(new ListProductsQuery(search, category, page, size));
        List<com.example.inventory.web.dto.ProductResponse> content = result.content().stream()
                .map(productMapper::toWebResponse).toList();
        return ResponseEntity.ok(new com.example.inventory.web.dto.ProductListResponse(content, result.total(),
                result.page(), result.size()));
    }

    @GetMapping("/products/{id}")
    @Operation(summary = "Get product details", description = "Fetches a single product by its identifier.")
    @ApiResponse(responseCode = "200", description = "Product found")
    @ApiResponse(responseCode = "404", description = "Product not found")
    public ResponseEntity<com.example.inventory.web.dto.ProductResponse> getProduct(@PathVariable String id) {
        var response = getProductQueryHandler.handle(new GetProductQuery(id));
        return ResponseEntity.ok(productMapper.toWebResponse(response));
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
