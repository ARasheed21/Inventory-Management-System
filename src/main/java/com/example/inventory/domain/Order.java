package com.example.inventory.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Order {
    private final String id;
    private final String customerId;
    private final List<OrderItem> items;
    private final Address shippingAddress;
    private OrderStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant reservedUntil;
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    public Order(String id, String customerId, List<OrderItem> items, Address shippingAddress) {
        this(id, customerId, items, shippingAddress, Instant.now());
    }

    public Order(String id, String customerId, List<OrderItem> items, Address shippingAddress, Instant createdAt) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Order id is required");
        }
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("Customer id is required");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("At least one order item is required");
        }
        if (shippingAddress == null) {
            throw new IllegalArgumentException("Shipping address is required");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("Created at is required");
        }
        this.id = id;
        this.customerId = customerId;
        this.items = new ArrayList<>(items);
        this.shippingAddress = shippingAddress;
        this.status = OrderStatus.PENDING;
        this.createdAt = createdAt;
        this.updatedAt = this.createdAt;
        this.reservedUntil = this.createdAt.plusSeconds(15 * 60);
        this.domainEvents.add(new OrderCreatedEvent(id, customerId, this.createdAt));
    }

    public static Order create(String customerId, List<OrderItem> items, Address shippingAddress) {
        return new Order(UUID.randomUUID().toString(), customerId, items, shippingAddress);
    }

    public static Order create(String id, String customerId, List<OrderItem> items, Instant createdAt) {
        return new Order(id, customerId, items,
                new Address("Default Street", "Default City", "Default State", "00000", "US"), createdAt);
    }

    public void pay() {
        markPaid();
    }

    public void markPaid() {
        ensureTransition(OrderStatus.PAID);
        this.status = OrderStatus.PAID;
        this.updatedAt = Instant.now();
        this.domainEvents.add(new OrderPaidEvent(id, this.updatedAt));
    }

    public void ship() {
        ensureTransition(OrderStatus.SHIPPED);
        this.status = OrderStatus.SHIPPED;
        this.updatedAt = Instant.now();
        this.domainEvents.add(new OrderShippedEvent(id, this.updatedAt));
    }

    public void deliver() {
        ensureTransition(OrderStatus.DELIVERED);
        this.status = OrderStatus.DELIVERED;
        this.updatedAt = Instant.now();
        this.domainEvents.add(new OrderDeliveredEvent(id, this.updatedAt));
    }

    public void cancel() {
        ensureTransition(OrderStatus.CANCELLED);
        this.status = OrderStatus.CANCELLED;
        this.updatedAt = Instant.now();
        this.domainEvents.add(new OrderCancelledEvent(id, this.updatedAt));
    }

    public void expireReservation() {
        if (this.status != OrderStatus.PENDING) {
            throw new IllegalStateException("Reservation can only expire while pending");
        }
        this.status = OrderStatus.CANCELLED;
        this.updatedAt = Instant.now();
        this.domainEvents.add(new ReservationExpiredEvent(id, this.updatedAt));
    }

    public boolean isReservationExpired(Instant now) {
        return this.status == OrderStatus.PENDING && now.isAfter(this.reservedUntil);
    }

    private void ensureTransition(OrderStatus target) {
        if (!this.status.canTransitionTo(target)) {
            throw new IllegalStateException("cannot transition from " + this.status + " to " + target);
        }
    }

    public String getId() {
        return id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public Address getShippingAddress() {
        return shippingAddress;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getReservedUntil() {
        return reservedUntil;
    }

    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    public void clearDomainEvents() {
        domainEvents.clear();
    }
}
