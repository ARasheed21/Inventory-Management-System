package com.example.inventory.domain.events;

import java.time.Instant;

public record ReservationExpiredEvent(String orderId, Instant occurredAt) implements DomainEvent {
}
