package com.example.inventory.application.handlers;

import com.example.inventory.application.commands.PlaceOrderCommand;
import com.example.inventory.application.dto.CreateOrderItemRequest;
import com.example.inventory.application.dto.CreateOrderRequest;
import com.example.inventory.application.dto.OrderResponse;
import com.example.inventory.domain.entities.Order;
import com.example.inventory.domain.entities.Product;
import com.example.inventory.domain.repositories.OrderRepository;
import com.example.inventory.domain.repositories.ProductRepository;
import com.example.inventory.domain.valueobjects.Address;
import com.example.inventory.domain.valueobjects.OrderItem;
import com.example.inventory.domain.valueobjects.SKU;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class PlaceOrderCommandHandler {
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public PlaceOrderCommandHandler(OrderRepository orderRepository, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    public OrderResponse handle(PlaceOrderCommand command) {
        CreateOrderRequest request = command.request();
        validateRequest(request);

        List<OrderItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (CreateOrderItemRequest itemRequest : request.items()) {
            Product product = productRepository.findById(itemRequest.productId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + itemRequest.productId()));
            if (product.getQuantityInStock() < itemRequest.quantity()) {
                throw new IllegalArgumentException("Insufficient inventory for product: " + itemRequest.productId());
            }
            items.add(new OrderItem(product.getId(), itemRequest.quantity(), product.getPrice(),
                    SKU.of(product.getId())));
            total = total.add(product.getPrice().amount().multiply(BigDecimal.valueOf(itemRequest.quantity())));
        }

        Order order = Order.create(request.customerId(), items,
                new Address("Default Street", "Default City", "Default State", "00000", "US"));
        orderRepository.save(order);
        return new OrderResponse(order.getId(), order.getCustomerId(), order.getStatus().name(), total, "USD",
                order.getCreatedAt(), order.getReservedUntil());
    }

    private void validateRequest(CreateOrderRequest request) {
        if (request == null || request.customerId() == null || request.customerId().isBlank()) {
            throw new IllegalArgumentException("Customer id is required");
        }
        if (request.items() == null || request.items().isEmpty()) {
            throw new IllegalArgumentException("At least one order item is required");
        }
    }
}
