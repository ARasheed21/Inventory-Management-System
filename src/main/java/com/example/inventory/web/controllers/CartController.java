package com.example.inventory.web.controllers;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.example.inventory.application.handlers.CartHandler;
import com.example.inventory.web.dto.CartItemRequest;
import com.example.inventory.web.dto.CartResponse;

@RestController
@RequestMapping({ "/api/cart", "/cart" })
@Tag(name = "Cart", description = "Shopping-cart endpoints scoped to the authenticated customer")
public class CartController {

    private final CartHandler cartHandler;

    public CartController(CartHandler cartHandler) {
        this.cartHandler = cartHandler;
    }

    @GetMapping
    @Operation(summary = "List cart items", description = "Returns all cart items owned by the authenticated user.")
    @ApiResponse(responseCode = "200", description = "Cart items returned (possibly empty)")
    public ResponseEntity<List<CartResponse>> getCart(Principal principal) {
        return ResponseEntity.ok(cartHandler.getCart(principal.getName()).stream()
                .map(item -> new CartResponse(item.id(), item.customerId(), item.productId(), item.quantity()))
                .toList());
    }

    @PostMapping
    @Operation(summary = "Add an item to the cart", description = "Creates a new cart item for the authenticated user.")
    @ApiResponse(responseCode = "200", description = "Cart item created")
    @ApiResponse(responseCode = "400", description = "Validation failed (blank productId, quantity < 1, unknown product)")
    public ResponseEntity<CartResponse> addCartItem(
            @Parameter(hidden = true) Principal principal,
            @Valid @RequestBody CartItemRequest request) {
        var item = cartHandler.addCartItem(principal.getName(), request.productId(), request.quantity());
        return ResponseEntity.ok(new CartResponse(item.id(), item.customerId(), item.productId(), item.quantity()));
    }

    @PutMapping("/{itemId}")
    @Operation(summary = "Update a cart item", description = "Updates quantity (and optionally product) of one of the authenticated user's cart items.")
    @ApiResponse(responseCode = "200", description = "Cart item updated")
    @ApiResponse(responseCode = "400", description = "Validation failed or quantity < 1")
    @ApiResponse(responseCode = "404", description = "Cart item not found for this user")
    public ResponseEntity<CartResponse> updateCartItem(
            @Parameter(hidden = true) Principal principal,
            @Parameter(description = "Cart item identifier") @PathVariable String itemId,
            @Valid @RequestBody com.example.inventory.web.dto.UpdateCartItemRequest request) {
        var item = cartHandler.updateCartItem(principal.getName(), itemId, request.productId(), request.quantity());
        return ResponseEntity.ok(new CartResponse(item.id(), item.customerId(), item.productId(), item.quantity()));
    }

    @DeleteMapping("/{itemId}")
    @Operation(summary = "Remove a cart item", description = "Deletes one of the authenticated user's cart items.")
    @ApiResponse(responseCode = "204", description = "Cart item removed")
    @ApiResponse(responseCode = "404", description = "Cart item not found for this user")
    public ResponseEntity<Void> deleteCartItem(
            @Parameter(hidden = true) Principal principal,
            @Parameter(description = "Cart item identifier") @PathVariable String itemId) {
        cartHandler.deleteCartItem(principal.getName(), itemId);
        return ResponseEntity.noContent().build();
    }
}
