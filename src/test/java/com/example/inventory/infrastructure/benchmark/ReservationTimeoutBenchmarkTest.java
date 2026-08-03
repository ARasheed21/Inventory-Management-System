package com.example.inventory.infrastructure.benchmark;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import com.example.inventory.domain.entities.Order;
import com.example.inventory.domain.repositories.OrderRepository;
import com.example.inventory.domain.valueobjects.Money;
import com.example.inventory.domain.valueobjects.OrderItem;
import com.example.inventory.domain.valueobjects.OrderStatus;
import com.example.inventory.domain.valueobjects.SKU;
import com.example.inventory.infrastructure.jobs.ReservationTimeoutJob;

@SpringBootTest
@ActiveProfiles("test")
class ReservationTimeoutBenchmarkTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ReservationTimeoutJob reservationTimeoutJob;

    @Test
    @Sql(scripts = "/reservation-benchmark-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void shouldCancelOneThousandPendingOrdersWithinFiveSeconds() {
        IntStream.range(0, 1000).forEach(index -> {
            Order order = Order.create(
                    "benchmark-order-" + index,
                    "customer-" + index,
                    List.of(new OrderItem("SKU-001", 1, Money.of("12.50", "USD"), SKU.of("SKU-001"))),
                    Instant.now().minusSeconds(20 * 60));
            orderRepository.save(order);
        });

        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> reservationTimeoutJob.processExpiredReservations());

        List<Order> orders = orderRepository.findAll();
        assertEquals(1000, orders.size());
        assertTrue(orders.stream().allMatch(order -> order.getStatus() == OrderStatus.CANCELLED));
    }
}
