package com.example.inventory.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemJpaEntityRepository extends JpaRepository<CartItemJpaEntity, Long> {
    Optional<CartItemJpaEntity> findByExternalId(String externalId);

    List<CartItemJpaEntity> findByCustomerId(String customerId);

    Optional<CartItemJpaEntity> findByCustomerIdAndProductId(String customerId, String productId);
}
