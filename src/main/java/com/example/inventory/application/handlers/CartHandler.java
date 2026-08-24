package com.example.inventory.application.handlers;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.example.inventory.domain.entities.CartItem;
import com.example.inventory.domain.repositories.CartRepository;
import com.example.inventory.domain.repositories.ProductRepository;

@Component
public class CartHandler {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;

    public CartHandler(CartRepository cartRepository, ProductRepository productRepository) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
    }

    public List<CartItem> getCart(String customerId) {
        return cartRepository.findByCustomerId(customerId);
    }

    public CartItem addCartItem(String customerId, String productId, int quantity) {
        validateProduct(productId, quantity);
        Optional<CartItem> existingItem = cartRepository.findByCustomerIdAndProductId(customerId, productId);
        CartItem item = existingItem
                .map(existing -> existing.withQuantity(existing.quantity() + quantity))
                .orElse(new CartItem(UUID.randomUUID().toString(), customerId, productId, quantity));
        return cartRepository.save(item);
    }

    public CartItem updateCartItem(String customerId, String itemId, String productId, int quantity) {
        CartItem item = cartRepository.findById(itemId)
                .filter(existing -> existing.customerId().equals(customerId))
                .orElseThrow(() -> new IllegalArgumentException("Cart item not found or access denied"));
        String effectiveProductId = productId != null && !productId.isBlank() ? productId : item.productId();
        validateProduct(effectiveProductId, quantity);
        if (!item.productId().equals(effectiveProductId)) {
            item = new CartItem(item.id(), customerId, effectiveProductId, quantity);
        } else {
            item = item.withQuantity(quantity);
        }
        return cartRepository.save(item);
    }

    public void deleteCartItem(String customerId, String itemId) {
        CartItem item = cartRepository.findById(itemId)
                .filter(existing -> existing.customerId().equals(customerId))
                .orElseThrow(() -> new IllegalArgumentException("Cart item not found or access denied"));
        cartRepository.deleteById(item.id());
    }

    private void validateProduct(String productId, int quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("Quantity must be at least 1");
        }
        productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
    }
}
