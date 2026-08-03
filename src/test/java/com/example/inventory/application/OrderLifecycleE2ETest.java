package com.example.inventory.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import com.example.inventory.application.commands.DeliverOrderCommand;
import com.example.inventory.application.commands.PlaceOrderCommand;
import com.example.inventory.application.commands.ProcessPaymentCommand;
import com.example.inventory.application.commands.ShipOrderCommand;
import com.example.inventory.application.dto.CreateOrderItemRequest;
import com.example.inventory.application.dto.CreateOrderRequest;
import com.example.inventory.application.dto.OrderResponse;
import com.example.inventory.application.handlers.DeliverOrderCommandHandler;
import com.example.inventory.application.handlers.PlaceOrderCommandHandler;
import com.example.inventory.application.handlers.ProcessPaymentCommandHandler;
import com.example.inventory.application.handlers.ShipOrderCommandHandler;
import com.example.inventory.domain.entities.Order;
import com.example.inventory.domain.repositories.OrderRepository;
import com.example.inventory.domain.valueobjects.OrderStatus;

@SpringBootTest
@ActiveProfiles("test")
class OrderLifecycleE2ETest {

    @Autowired
    private PlaceOrderCommandHandler placeOrderCommandHandler;

    @Autowired
    private ProcessPaymentCommandHandler processPaymentCommandHandler;

    @Autowired
    private ShipOrderCommandHandler shipOrderCommandHandler;

    @Autowired
    private DeliverOrderCommandHandler deliverOrderCommandHandler;

    @Autowired
    private OrderRepository orderRepository;

    @Test
    @Sql(scripts = "/order-lifecycle-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void shouldCompleteFullOrderLifecycleThroughHandlers() {
        OrderResponse created = placeOrderCommandHandler.handle(new PlaceOrderCommand(
                new CreateOrderRequest("customer-lifecycle", List.of(new CreateOrderItemRequest("SKU-001", 2)))));

        OrderResponse paid = processPaymentCommandHandler.handle(new ProcessPaymentCommand(created.id()));
        OrderResponse shipped = shipOrderCommandHandler.handle(new ShipOrderCommand(created.id()));
        OrderResponse delivered = deliverOrderCommandHandler.handle(new DeliverOrderCommand(created.id()));

        assertEquals("PENDING", created.status());
        assertEquals("PAID", paid.status());
        assertEquals("SHIPPED", shipped.status());
        assertEquals("DELIVERED", delivered.status());

        Order reloaded = orderRepository.findById(created.id()).orElseThrow(AssertionError::new);
        assertEquals(OrderStatus.DELIVERED, reloaded.getStatus());
    }
}
