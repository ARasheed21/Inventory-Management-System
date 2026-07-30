package com.example.inventory.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.inventory.domain.aggregates.OrderAggregate;
import com.example.inventory.domain.services.OrderDomainService;
import com.example.inventory.domain.valueobjects.Money;
import com.example.inventory.domain.valueobjects.OrderItem;
import com.example.inventory.domain.valueobjects.OrderStatus;
import com.example.inventory.domain.valueobjects.SKU;

class OrderDomainServiceTest {

    @Test
    void shouldExposeAggregateRootBehavior() {
        OrderAggregate aggregate = OrderAggregate.create(
                "customer-1",
                List.of(new OrderItem("product-1", 2, Money.of("10.00", "USD"), SKU.of("SKU-1"))));

        aggregate.pay();

        assertEquals(OrderStatus.PAID, aggregate.getStatus());
    }

    @Test
    void shouldExpireReservationWhenDeadlineHasPassed() {
        OrderAggregate aggregate = OrderAggregate.create(
                "customer-2",
                List.of(new OrderItem("product-2", 1, Money.of("5.00", "USD"), SKU.of("SKU-2"))));
        OrderDomainService service = new OrderDomainService();

        service.expireReservationIfNeeded(aggregate, aggregate.getReservedUntil().plusSeconds(1));

        assertEquals(OrderStatus.CANCELLED, aggregate.getStatus());
        assertTrue(aggregate.getDomainEvents().stream()
                .anyMatch(event -> event.getClass().getSimpleName().contains("Reservation")));
    }

    @Test
    void shouldNotExpireReservationBeforeDeadline() {
        OrderAggregate aggregate = OrderAggregate.create(
                "customer-3",
                List.of(new OrderItem("product-3", 1, Money.of("7.50", "USD"), SKU.of("SKU-3"))));
        OrderDomainService service = new OrderDomainService();

        service.expireReservationIfNeeded(aggregate, aggregate.getReservedUntil().minusSeconds(1));

        assertEquals(OrderStatus.PENDING, aggregate.getStatus());
        assertFalse(aggregate.getDomainEvents().stream()
                .anyMatch(event -> event.getClass().getSimpleName().contains("Reservation")));
    }
}
