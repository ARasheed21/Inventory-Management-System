package com.example.inventory.web.controllers;

import java.security.Principal;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
// PreAuthorize not used here; left commented intentionally if future role checks are added
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.inventory.application.handlers.CartHandler;
import com.example.inventory.web.dto.CartItemRequest;
import com.example.inventory.web.dto.CartResponse;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartHandler cartHandler;

    public CartController(CartHandler cartHandler) {
        this.cartHandler = cartHandler;
    }

    @GetMapping
    public ResponseEntity<List<CartResponse>> getCart(Principal principal) {
        return ResponseEntity.ok(cartHandler.getCart(principal.getName()).stream()
                .map(item -> new CartResponse(item.id(), item.customerId(), item.productId(), item.quantity()))
                .toList());
    }

    @PostMapping
    public ResponseEntity<CartResponse> addCartItem(Principal principal, @Valid @RequestBody CartItemRequest request) {
        var item = cartHandler.addCartItem(principal.getName(), request.productId(), request.quantity());
        return ResponseEntity.ok(new CartResponse(item.id(), item.customerId(), item.productId(), item.quantity()));
    }

    @PutMapping("/{itemId}")
    public ResponseEntity<CartResponse> updateCartItem(Principal principal, @PathVariable String itemId,
            @Valid @RequestBody CartItemRequest request) {
        var item = cartHandler.updateCartItem(principal.getName(), itemId, request.productId(), request.quantity());
        return ResponseEntity.ok(new CartResponse(item.id(), item.customerId(), item.productId(), item.quantity()));
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<Void> deleteCartItem(Principal principal, @PathVariable String itemId) {
        cartHandler.deleteCartItem(principal.getName(), itemId);
        return ResponseEntity.noContent().build();
    }
}
