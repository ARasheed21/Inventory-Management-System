package com.example.inventory.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.example.inventory.domain.entities.Order;
import com.example.inventory.domain.entities.Product;
import com.example.inventory.domain.repositories.OrderRepository;
import com.example.inventory.domain.repositories.ProductRepository;
import com.example.inventory.domain.valueobjects.Money;
import com.example.inventory.domain.valueobjects.OrderItem;
import com.example.inventory.domain.valueobjects.OrderStatus;
import com.example.inventory.domain.valueobjects.SKU;
import com.example.inventory.infrastructure.persistence.config.PersistenceConfig;
import com.example.inventory.infrastructure.persistence.jpa.JpaOrderRepository;
import com.example.inventory.infrastructure.persistence.jpa.JpaProductRepository;
import com.example.inventory.infrastructure.persistence.jpa.ProductJpaEntityRepository;

@DataJpaTest
@Import({ PersistenceConfig.class, JpaOrderRepository.class, JpaProductRepository.class })
@ActiveProfiles("test")
class JpaPersistenceIntegrationTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductJpaEntityRepository productJpaEntityRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void shouldPersistAndReloadOrderWithItems() {
        Order order = Order.create(
                "order-123",
                "customer-123",
                List.of(new OrderItem("product-123", 2, Money.of("10.00", "USD"), SKU.of("SKU-1"))),
                Instant.parse("2026-01-01T10:00:00Z"));

        order.pay();

        Order saved = orderRepository.save(order);
        Order reloaded = orderRepository.findById(saved.getId()).orElseThrow(AssertionError::new);

        assertEquals(OrderStatus.PAID, reloaded.getStatus());
        assertEquals(1, reloaded.getItems().size());
        assertEquals("customer-123", reloaded.getCustomerId());
    }

    @Test
    void shouldReloadProductDomainEntity() {
        Product product = new Product(
                "product-123",
                "Laptop",
                "Gaming laptop",
                Money.of("1999.99", "USD"),
                10,
                0L);

        productJpaEntityRepository.save(new com.example.inventory.infrastructure.persistence.jpa.ProductJpaEntity(
                "product-123",
                product.getName(),
                product.getDescription(),
                product.getPrice().amount(),
                product.getPrice().currency(),
                product.getQuantityInStock(),
                product.getVersion()));

        Product reloaded = productRepository.findById("product-123").orElseThrow(AssertionError::new);

        assertEquals("Laptop", reloaded.getName());
        assertEquals(10, reloaded.getQuantityInStock());
        assertEquals(new BigDecimal("1999.99"), reloaded.getPrice().amount());
    }

    @Test
    void shouldThrowOptimisticLockExceptionOnConcurrentUpdate() {
        Order order = Order.create(
                "order-456",
                "customer-456",
                List.of(new OrderItem("product-456", 1, Money.of("5.00", "USD"), SKU.of("SKU-2"))),
                Instant.parse("2026-01-02T10:00:00Z"));

        EntityManager initialEntityManager = entityManagerFactory.createEntityManager();
        initialEntityManager.getTransaction().begin();
        com.example.inventory.infrastructure.persistence.jpa.OrderJpaEntity persisted = new com.example.inventory.infrastructure.persistence.jpa.OrderJpaEntity(
                order.getId(),
                order.getCustomerId(),
                order.getStatus().name(),
                BigDecimal.valueOf(5.00),
                "USD",
                order.getCreatedAt(),
                order.getUpdatedAt(),
                order.getReservedUntil());
        initialEntityManager.persist(persisted);
        initialEntityManager.flush();
        initialEntityManager.getTransaction().commit();
        initialEntityManager.close();

        assertTrue(persisted.getId() != null);

        EntityManager firstEntityManager = entityManagerFactory.createEntityManager();
        EntityManager secondEntityManager = entityManagerFactory.createEntityManager();

        try {
            firstEntityManager.getTransaction().begin();
            secondEntityManager.getTransaction().begin();

            com.example.inventory.infrastructure.persistence.jpa.OrderJpaEntity first = firstEntityManager.find(
                    com.example.inventory.infrastructure.persistence.jpa.OrderJpaEntity.class, persisted.getId());
            com.example.inventory.infrastructure.persistence.jpa.OrderJpaEntity second = secondEntityManager.find(
                    com.example.inventory.infrastructure.persistence.jpa.OrderJpaEntity.class, persisted.getId());

            first.setStatus(OrderStatus.PAID.name());
            firstEntityManager.flush();
            firstEntityManager.getTransaction().commit();

            second.setStatus(OrderStatus.CANCELLED.name());
            second.setUpdatedAt(Instant.now());

            assertThrows(jakarta.persistence.OptimisticLockException.class, () -> {
                secondEntityManager.flush();
                secondEntityManager.getTransaction().commit();
            });
        } finally {
            if (firstEntityManager.getTransaction().isActive()) {
                firstEntityManager.getTransaction().rollback();
            }
            if (secondEntityManager.getTransaction().isActive()) {
                secondEntityManager.getTransaction().rollback();
            }
            firstEntityManager.close();
            secondEntityManager.close();
        }
    }
}
