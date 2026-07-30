package com.example.inventory.domain.events;

import java.time.Instant;

public record OrderCreatedEvent(String orderId, String customerId, Instant occurredAt) implements DomainEvent {
}
