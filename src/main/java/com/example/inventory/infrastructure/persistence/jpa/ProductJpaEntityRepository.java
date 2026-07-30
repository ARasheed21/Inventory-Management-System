package com.example.inventory.infrastructure.persistence.jpa;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductJpaEntityRepository extends JpaRepository<ProductJpaEntity, Long> {
    Optional<ProductJpaEntity> findByExternalId(String externalId);
}
