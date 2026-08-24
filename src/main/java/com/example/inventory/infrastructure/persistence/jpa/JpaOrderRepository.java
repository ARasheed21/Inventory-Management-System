package com.example.inventory.infrastructure.persistence.jpa;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.example.inventory.domain.entities.Order;
import com.example.inventory.domain.repositories.OrderRepository;

@Repository
public class JpaOrderRepository implements OrderRepository {
    private final OrderJpaEntityRepository orderJpaEntityRepository;
    private final OrderJpaMapper orderJpaMapper;

    @PersistenceContext
    private EntityManager entityManager;

    public JpaOrderRepository(OrderJpaEntityRepository orderJpaEntityRepository) {
        this.orderJpaEntityRepository = orderJpaEntityRepository;
        this.orderJpaMapper = new OrderJpaMapper();
    }

    @Override
    @Transactional
    public Order save(Order order) {
        OrderJpaEntity entity = orderJpaEntityRepository.findByExternalId(order.getId()).orElse(null);
        OrderJpaEntity mappedEntity = orderJpaMapper.toEntity(order);

        if (entity == null) {
            OrderJpaEntity persisted = orderJpaEntityRepository.saveAndFlush(mappedEntity);
            return orderJpaMapper.toDomain(persisted);
        }

        entity.setStatus(mappedEntity.getStatus());
        entity.setUpdatedAt(mappedEntity.getUpdatedAt());
        entity.setItems(mappedEntity.getItems());
        entity.setReservedUntil(mappedEntity.getReservedUntil());
        entity.setTotalAmount(mappedEntity.getTotalAmount());
        entity.setCurrency(mappedEntity.getCurrency());
        entity.setCustomerId(mappedEntity.getCustomerId());
        entity.setCreatedAt(mappedEntity.getCreatedAt());
        return orderJpaMapper.toDomain(orderJpaEntityRepository.saveAndFlush(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Order> findById(String id) {
        return orderJpaEntityRepository.findByExternalId(id).map(orderJpaMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> findByCustomerId(String customerId) {
        return orderJpaEntityRepository.findAll().stream()
                .filter(entity -> customerId.equals(entity.getCustomerId()))
                .map(orderJpaMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> findAll() {
        return orderJpaEntityRepository.findAll().stream().map(orderJpaMapper::toDomain).toList();
    }

    @Override
    @Transactional
    public List<Order> cancelExpiredPendingOrders(Instant now) {
        List<OrderJpaEntity> expired = entityManager.createQuery(
                "SELECT o FROM OrderJpaEntity o WHERE o.status = :pendingStatus AND o.reservedUntil < :now",
                OrderJpaEntity.class)
                .setParameter("pendingStatus", com.example.inventory.domain.valueobjects.OrderStatus.PENDING.name())
                .setParameter("now", now)
                .getResultList();
        for (OrderJpaEntity entity : expired) {
            entity.setStatus(com.example.inventory.domain.valueobjects.OrderStatus.CANCELLED.name());
            entity.setUpdatedAt(now);
        }
        return expired.stream().map(orderJpaMapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Map<String, Integer> findReservedQuantitiesByProduct() {
        List<Object[]> rows = entityManager.createQuery(
                "SELECT i.productId, SUM(i.quantity) FROM OrderJpaEntity o JOIN o.items i "
                        + "WHERE o.status = :pendingStatus GROUP BY i.productId", Object[].class)
                .setParameter("pendingStatus", com.example.inventory.domain.valueobjects.OrderStatus.PENDING.name())
                .getResultList();
        java.util.Map<String, Integer> reserved = new java.util.HashMap<>();
        for (Object[] row : rows) {
            reserved.put((String) row[0], ((Number) row[1]).intValue());
        }
        return reserved;
    }

    @Override
    @Transactional
    public void delete(String id) {
        orderJpaEntityRepository.findByExternalId(id).ifPresent(orderJpaEntityRepository::delete);
    }
}
