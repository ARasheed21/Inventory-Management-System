package com.example.inventory.domain.events;

import java.time.Instant;

public record OrderDeliveredEvent(String orderId, Instant occurredAt) implements DomainEvent {
}
