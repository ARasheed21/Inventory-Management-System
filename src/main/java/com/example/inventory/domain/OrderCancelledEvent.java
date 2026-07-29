package com.example.inventory.domain;

import java.time.Instant;

public record OrderCancelledEvent(String orderId, Instant occurredAt) implements DomainEvent {
}
