package com.example.inventory.domain.events;

import java.time.Instant;

public record OrderPaidEvent(String orderId, Instant occurredAt) implements DomainEvent {
}
