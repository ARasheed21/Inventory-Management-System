package com.example.inventory.infrastructure.persistence.jpa;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.example.inventory.domain.entities.Order;
import com.example.inventory.domain.valueobjects.Address;
import com.example.inventory.domain.valueobjects.Money;
import com.example.inventory.domain.valueobjects.OrderItem;
import com.example.inventory.domain.valueobjects.OrderStatus;
import com.example.inventory.domain.valueobjects.SKU;

public class OrderJpaMapper {

    public OrderJpaEntity toEntity(Order order) {
        OrderJpaEntity entity = new OrderJpaEntity(order.getId(), order.getCustomerId(), order.getStatus().name(),
                calculateTotalAmount(order), "USD", order.getCreatedAt(), order.getUpdatedAt(),
                order.getReservedUntil());

        List<OrderItemJpaEntity> itemEntities = new ArrayList<>();
        for (OrderItem item : order.getItems()) {
            itemEntities.add(new OrderItemJpaEntity(entity, item.productId(), item.quantity(),
                    item.unitPrice().amount(), item.sku().value()));
        }
        entity.setItems(itemEntities);
        return entity;
    }

    public Order toDomain(OrderJpaEntity entity) {
        List<OrderItem> items = entity.getItems().stream()
                .map(item -> new OrderItem(item.getProductId(), item.getQuantity(),
                        Money.of(item.getUnitPrice().toPlainString(), "USD"), SKU.of(item.getSku())))
                .toList();

        Order order = new Order(entity.getExternalId(), entity.getCustomerId(), items,
                new Address("Default Street", "Default City", "Default State", "00000", "US"), entity.getCreatedAt());
        if (OrderStatus.valueOf(entity.getStatus()) == OrderStatus.PAID) {
            order.pay();
        } else if (OrderStatus.valueOf(entity.getStatus()) == OrderStatus.SHIPPED) {
            order.pay();
            order.ship();
        } else if (OrderStatus.valueOf(entity.getStatus()) == OrderStatus.DELIVERED) {
            order.pay();
            order.ship();
            order.deliver();
        } else if (OrderStatus.valueOf(entity.getStatus()) == OrderStatus.CANCELLED) {
            order.cancel();
        }
        return order;
    }

    private BigDecimal calculateTotalAmount(Order order) {
        return order.getItems().stream()
                .map(item -> item.unitPrice().amount().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
