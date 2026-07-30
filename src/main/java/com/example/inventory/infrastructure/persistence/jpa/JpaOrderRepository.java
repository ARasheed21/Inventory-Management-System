package com.example.inventory.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.example.inventory.domain.entities.Order;
import com.example.inventory.domain.repositories.OrderRepository;

@Repository
public class JpaOrderRepository implements OrderRepository {
    private final OrderJpaEntityRepository orderJpaEntityRepository;
    private final OrderJpaMapper orderJpaMapper;

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
    public void delete(String id) {
        orderJpaEntityRepository.findByExternalId(id).ifPresent(orderJpaEntityRepository::delete);
    }
}
