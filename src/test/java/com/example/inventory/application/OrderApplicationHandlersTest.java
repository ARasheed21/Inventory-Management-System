package com.example.inventory.application;

import com.example.inventory.application.commands.CancelOrderCommand;
import com.example.inventory.application.commands.PlaceOrderCommand;
import com.example.inventory.application.commands.ProcessPaymentCommand;
import com.example.inventory.application.dto.CreateOrderItemRequest;
import com.example.inventory.application.dto.CreateOrderRequest;
import com.example.inventory.application.dto.InventoryItemResponse;
import com.example.inventory.application.dto.OrderResponse;
import com.example.inventory.application.handlers.CancelOrderCommandHandler;
import com.example.inventory.application.handlers.GetInventoryQueryHandler;
import com.example.inventory.application.handlers.GetOrderQueryHandler;
import com.example.inventory.application.handlers.PlaceOrderCommandHandler;
import com.example.inventory.application.handlers.ProcessPaymentCommandHandler;
import com.example.inventory.application.queries.GetInventoryQuery;
import com.example.inventory.application.queries.GetOrderQuery;
import com.example.inventory.domain.Money;
import com.example.inventory.domain.Order;
import com.example.inventory.domain.OrderRepository;
import com.example.inventory.domain.OrderStatus;
import com.example.inventory.domain.Product;
import com.example.inventory.domain.ProductRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderApplicationHandlersTest {

    @Test
    void shouldCreatePendingOrderFromPlaceOrderCommand() {
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        InMemoryProductRepository productRepository = new InMemoryProductRepository();
        PlaceOrderCommandHandler handler = new PlaceOrderCommandHandler(orderRepository, productRepository);

        CreateOrderRequest request = new CreateOrderRequest(
                "customer-1",
                List.of(new CreateOrderItemRequest("product-1", 2)));

        OrderResponse response = handler.handle(new PlaceOrderCommand(request));

        assertEquals(OrderStatus.PENDING.name(), response.status());
        assertEquals("customer-1", response.customerId());
        assertEquals(new BigDecimal("19.98"), response.totalAmount());
        assertEquals(1, orderRepository.savedOrders.size());
    }

    @Test
    void shouldMarkOrderAsPaid() {
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        InMemoryProductRepository productRepository = new InMemoryProductRepository();
        PlaceOrderCommandHandler placeHandler = new PlaceOrderCommandHandler(orderRepository, productRepository);
        ProcessPaymentCommandHandler paymentHandler = new ProcessPaymentCommandHandler(orderRepository);

        OrderResponse created = placeHandler.handle(new PlaceOrderCommand(
                new CreateOrderRequest("c-1", List.of(new CreateOrderItemRequest("product-1", 1)))));

        OrderResponse updated = paymentHandler.handle(new ProcessPaymentCommand(created.id()));

        assertEquals(OrderStatus.PAID.name(), updated.status());
    }

    @Test
    void shouldCancelPendingOrder() {
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        InMemoryProductRepository productRepository = new InMemoryProductRepository();
        PlaceOrderCommandHandler placeHandler = new PlaceOrderCommandHandler(orderRepository, productRepository);
        CancelOrderCommandHandler cancelHandler = new CancelOrderCommandHandler(orderRepository);

        OrderResponse created = placeHandler.handle(new PlaceOrderCommand(
                new CreateOrderRequest("c-1", List.of(new CreateOrderItemRequest("product-1", 1)))));
        OrderResponse updated = cancelHandler.handle(new CancelOrderCommand(created.id()));

        assertEquals(OrderStatus.CANCELLED.name(), updated.status());
    }

    @Test
    void shouldQueryOrderAndInventory() {
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        InMemoryProductRepository productRepository = new InMemoryProductRepository();
        PlaceOrderCommandHandler placeHandler = new PlaceOrderCommandHandler(orderRepository, productRepository);
        GetOrderQueryHandler getOrderHandler = new GetOrderQueryHandler(orderRepository);
        GetInventoryQueryHandler inventoryHandler = new GetInventoryQueryHandler(productRepository);

        OrderResponse created = placeHandler.handle(new PlaceOrderCommand(
                new CreateOrderRequest("c-1", List.of(new CreateOrderItemRequest("product-1", 1)))));

        OrderResponse orderResponse = getOrderHandler.handle(new GetOrderQuery(created.id()));
        InventoryItemResponse inventory = inventoryHandler.handle(new GetInventoryQuery());

        assertEquals(created.id(), orderResponse.id());
        assertEquals("product-1", inventory.productId());
        assertEquals(10, inventory.quantityInStock());
    }

    @Test
    void shouldRejectOrderWhenProductIsUnavailable() {
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        InMemoryProductRepository productRepository = new InMemoryProductRepository();
        productRepository.products.clear();
        productRepository.products.add(new Product("product-1", "Widget", "desc", new Money("10.00", "USD"), 0, 1));
        PlaceOrderCommandHandler handler = new PlaceOrderCommandHandler(orderRepository, productRepository);

        assertThrows(IllegalArgumentException.class, () -> handler.handle(new PlaceOrderCommand(
                new CreateOrderRequest("c-1", List.of(new CreateOrderItemRequest("product-1", 1))))));
    }

    private static class InMemoryOrderRepository implements OrderRepository {
        private final List<Order> savedOrders = new ArrayList<>();

        @Override
        public Order save(Order order) {
            savedOrders.removeIf(existing -> existing.getId().equals(order.getId()));
            savedOrders.add(order);
            return order;
        }

        @Override
        public Optional<Order> findById(String id) {
            return savedOrders.stream().filter(order -> order.getId().equals(id)).findFirst();
        }

        @Override
        public List<Order> findAll() {
            return List.copyOf(savedOrders);
        }

        @Override
        public List<Order> findByCustomerId(String customerId) {
            return savedOrders.stream().filter(order -> order.getCustomerId().equals(customerId)).toList();
        }

        @Override
        public void delete(String id) {
            savedOrders.removeIf(order -> order.getId().equals(id));
        }
    }

    private static class InMemoryProductRepository implements ProductRepository {
        private final List<Product> products = new ArrayList<>(List.of(
                new Product("product-1", "Widget", "A sample widget", new Money("9.99", "USD"), 10, 1)));

        @Override
        public Optional<Product> findById(String id) {
            return products.stream().filter(product -> product.getId().equals(id)).findFirst();
        }

        @Override
        public List<Product> findAll() {
            return List.copyOf(products);
        }
    }
}
