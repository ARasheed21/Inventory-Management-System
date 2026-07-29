package com.example.inventory.domain;

import java.time.Instant;

public record OrderDeliveredEvent(String orderId, Instant occurredAt) implements DomainEvent {
}
