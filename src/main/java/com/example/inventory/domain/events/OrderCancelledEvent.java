package com.example.inventory.domain.events;

import java.time.Instant;

public record OrderCancelledEvent(String orderId, Instant occurredAt) implements DomainEvent {
}
