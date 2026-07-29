package com.example.inventory.domain;

import java.time.Instant;

public record OrderShippedEvent(String orderId, Instant occurredAt) implements DomainEvent {
}
