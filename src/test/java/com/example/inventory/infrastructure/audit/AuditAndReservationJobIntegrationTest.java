package com.example.inventory.infrastructure.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.Instant;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.example.inventory.domain.entities.Order;
import com.example.inventory.domain.repositories.OrderRepository;
import com.example.inventory.domain.valueobjects.Money;
import com.example.inventory.domain.valueobjects.OrderItem;
import com.example.inventory.domain.valueobjects.OrderStatus;
import com.example.inventory.domain.valueobjects.SKU;
import com.example.inventory.infrastructure.jobs.ReservationTimeoutJob;
import com.example.inventory.infrastructure.persistence.jpa.OrderJpaEntity;
import com.example.inventory.infrastructure.persistence.jpa.OrderJpaEntityRepository;

@SpringBootTest
@ActiveProfiles("test")
class AuditAndReservationJobIntegrationTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderJpaEntityRepository orderJpaEntityRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private ReservationTimeoutJob reservationTimeoutJob;

    @Test
    void shouldWriteAuditRevisionWhenOrderIsUpdated() {
        Order order = Order.create(
                "order-audit",
                "customer-audit",
                List.of(new OrderItem("product-audit", 1, Money.of("10.00", "USD"), SKU.of("SKU-A"))),
                Instant.parse("2026-02-01T10:00:00Z"));

        Order saved = orderRepository.save(order);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            entityManager.getTransaction().begin();
            OrderJpaEntity persisted = entityManager.find(OrderJpaEntity.class, orderJpaEntityRepository
                    .findByExternalId(saved.getId())
                    .orElseThrow(AssertionError::new)
                    .getId());
            persisted.setStatus(OrderStatus.PAID.name());
            entityManager.flush();
            entityManager.getTransaction().commit();
        } finally {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            entityManager.close();
        }

        EntityManager auditEntityManager = entityManagerFactory.createEntityManager();
        try {
            AuditReader auditReader = AuditReaderFactory.get(auditEntityManager);
            OrderJpaEntity auditedEntity = auditEntityManager.find(OrderJpaEntity.class, orderJpaEntityRepository
                    .findByExternalId(saved.getId())
                    .orElseThrow(AssertionError::new)
                    .getId());
            List<Number> revisions = auditReader.getRevisions(OrderJpaEntity.class, auditedEntity.getId());
            assertFalse(revisions.isEmpty());
        } finally {
            auditEntityManager.close();
        }
    }

    @Test
    void shouldCancelExpiredPendingOrder() {
        Order expiredOrder = Order.create(
                "order-expired",
                "customer-expired",
                List.of(new OrderItem("product-expired", 1, Money.of("15.00", "USD"), SKU.of("SKU-E"))),
                Instant.now().minusSeconds(20 * 60));

        Order saved = orderRepository.save(expiredOrder);

        reservationTimeoutJob.processExpiredReservations();

        Order reloaded = orderRepository.findById(saved.getId()).orElseThrow(AssertionError::new);
        assertEquals(OrderStatus.CANCELLED, reloaded.getStatus());
    }

    @Test
    void shouldLeaveNonExpiredPendingOrderPending() {
        Order activeOrder = Order.create(
                "order-active",
                "customer-active",
                List.of(new OrderItem("product-active", 1, Money.of("25.00", "USD"), SKU.of("SKU-ACTIVE"))),
                Instant.now().minusSeconds(5 * 60));

        Order saved = orderRepository.save(activeOrder);

        reservationTimeoutJob.processExpiredReservations();

        Order reloaded = orderRepository.findById(saved.getId()).orElseThrow(AssertionError::new);
        assertEquals(OrderStatus.PENDING, reloaded.getStatus());
    }
}
