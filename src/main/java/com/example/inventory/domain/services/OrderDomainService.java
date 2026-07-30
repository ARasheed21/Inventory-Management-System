package com.example.inventory.domain.services;

import java.time.Instant;

import com.example.inventory.domain.aggregates.OrderAggregate;
import com.example.inventory.domain.valueobjects.OrderStatus;

public class OrderDomainService {

    public void expireReservationIfNeeded(OrderAggregate aggregate, Instant now) {
        if (aggregate.isReservationExpired(now)) {
            aggregate.expireReservation();
        }
    }

    public boolean canBeCompleted(OrderAggregate aggregate) {
        return aggregate.getStatus() == OrderStatus.PAID;
    }
}
