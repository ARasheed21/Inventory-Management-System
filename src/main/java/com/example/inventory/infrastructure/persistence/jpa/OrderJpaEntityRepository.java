package com.example.inventory.infrastructure.persistence.jpa;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderJpaEntityRepository extends JpaRepository<OrderJpaEntity, Long> {
    Optional<OrderJpaEntity> findByExternalId(String externalId);
}
