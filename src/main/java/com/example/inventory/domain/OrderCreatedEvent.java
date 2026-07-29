package com.example.inventory.domain;

import java.time.Instant;

public record OrderCreatedEvent(String orderId, String customerId, Instant occurredAt) implements DomainEvent {
}
