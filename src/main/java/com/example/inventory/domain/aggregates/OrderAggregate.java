package com.example.inventory.domain.aggregates;

import java.time.Instant;
import java.util.List;

import com.example.inventory.domain.entities.Order;
import com.example.inventory.domain.valueobjects.Address;
import com.example.inventory.domain.valueobjects.OrderItem;
import com.example.inventory.domain.valueobjects.OrderStatus;

public class OrderAggregate {
    private final Order order;

    private OrderAggregate(Order order) {
        this.order = order;
    }

    public static OrderAggregate create(String customerId, List<OrderItem> items) {
        return new OrderAggregate(Order.create(customerId, items,
                new Address("Default Street", "Default City", "Default State", "00000", "US")));
    }

    public static OrderAggregate create(String id, String customerId, List<OrderItem> items, Instant createdAt) {
        return new OrderAggregate(Order.create(id, customerId, items, createdAt));
    }

    public void pay() {
        order.pay();
    }

    public void ship() {
        order.ship();
    }

    public void deliver() {
        order.deliver();
    }

    public void cancel() {
        order.cancel();
    }

    public void expireReservation() {
        order.expireReservation();
    }

    public boolean isReservationExpired(Instant now) {
        return order.isReservationExpired(now);
    }

    public String getId() {
        return order.getId();
    }

    public String getCustomerId() {
        return order.getCustomerId();
    }

    public List<OrderItem> getItems() {
        return order.getItems();
    }

    public Address getShippingAddress() {
        return order.getShippingAddress();
    }

    public OrderStatus getStatus() {
        return order.getStatus();
    }

    public Instant getCreatedAt() {
        return order.getCreatedAt();
    }

    public Instant getUpdatedAt() {
        return order.getUpdatedAt();
    }

    public Instant getReservedUntil() {
        return order.getReservedUntil();
    }

    public List<Object> getDomainEvents() {
        return order.getDomainEvents().stream().map(event -> (Object) event).toList();
    }
}
