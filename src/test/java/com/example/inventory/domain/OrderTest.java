package com.example.inventory.domain;

import org.junit.jupiter.api.Test;

import com.example.inventory.domain.entities.Order;
import com.example.inventory.domain.valueobjects.Money;
import com.example.inventory.domain.valueobjects.OrderItem;
import com.example.inventory.domain.valueobjects.OrderStatus;
import com.example.inventory.domain.valueobjects.SKU;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderTest {

    @Test
    void shouldCreatePendingOrderWithReservationWindow() {
        Instant createdAt = Instant.parse("2026-01-01T10:00:00Z");
        Order order = Order.create(
                "order-1",
                "customer-1",
                List.of(new OrderItem("product-1", 2, Money.of("10.00", "USD"), SKU.of("SKU-1"))),
                createdAt);

        assertEquals(OrderStatus.PENDING, order.getStatus());
        assertEquals(createdAt.plusSeconds(15 * 60), order.getReservedUntil());
        assertFalse(order.isReservationExpired(createdAt.plusSeconds(10 * 60)));
    }

    @Test
    void shouldTransitionPendingOrderToPaid() {
        Order order = createDefaultOrder();

        order.pay();

        assertEquals(OrderStatus.PAID, order.getStatus());
    }

    @Test
    void shouldTransitionPaidOrderToShippedAndDelivered() {
        Order order = createDefaultOrder();
        order.pay();
        order.ship();
        order.deliver();

        assertEquals(OrderStatus.DELIVERED, order.getStatus());
    }

    @Test
    void shouldCancelPendingOrder() {
        Order order = createDefaultOrder();

        order.cancel();

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
    }

    @Test
    void shouldRejectInvalidTransition() {
        Order order = createDefaultOrder();
        order.pay();

        IllegalStateException exception = assertThrows(IllegalStateException.class, order::cancel);
        assertTrue(exception.getMessage().contains("cannot transition"));
    }

    @Test
    void shouldDetectExpiredReservation() {
        Order order = createDefaultOrder();

        assertTrue(order.isReservationExpired(order.getReservedUntil().plusSeconds(1)));
    }

    @Test
    void shouldCreateMoneyWithDecimalAmount() {
        Money money = Money.of("10.50", "USD");

        assertEquals(new BigDecimal("10.50"), money.amount());
        assertEquals("USD", money.currency());
    }

    private Order createDefaultOrder() {
        return Order.create(
                "order-2",
                "customer-2",
                List.of(new OrderItem("product-2", 1, Money.of("5.00", "USD"), SKU.of("SKU-2"))),
                Instant.parse("2026-01-01T10:00:00Z"));
    }
}
