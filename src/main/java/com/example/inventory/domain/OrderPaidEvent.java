package com.example.inventory.domain;

import java.time.Instant;

public record OrderPaidEvent(String orderId, Instant occurredAt) implements DomainEvent {
}
