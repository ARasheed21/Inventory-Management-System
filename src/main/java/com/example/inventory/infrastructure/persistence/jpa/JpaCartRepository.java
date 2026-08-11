package com.example.inventory.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.example.inventory.domain.entities.CartItem;
import com.example.inventory.domain.repositories.CartRepository;

@Repository
public class JpaCartRepository implements CartRepository {

    private final CartItemJpaEntityRepository repository;

    public JpaCartRepository(CartItemJpaEntityRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CartItem> findByCustomerId(String customerId) {
        return repository.findByCustomerId(customerId).stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CartItem> findById(String itemId) {
        return repository.findByExternalId(itemId).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CartItem> findByCustomerIdAndProductId(String customerId, String productId) {
        return repository.findByCustomerIdAndProductId(customerId, productId).map(this::toDomain);
    }

    @Override
    @Transactional
    public CartItem save(CartItem cartItem) {
        CartItemJpaEntity entity = repository.findByExternalId(cartItem.id()).orElse(null);
        if (entity == null) {
            entity = repository.saveAndFlush(new CartItemJpaEntity(cartItem.id(), cartItem.customerId(),
                    cartItem.productId(), cartItem.quantity()));
        } else {
            entity.setCustomerId(cartItem.customerId());
            entity.setProductId(cartItem.productId());
            entity.setQuantity(cartItem.quantity());
            entity = repository.saveAndFlush(entity);
        }
        return toDomain(entity);
    }

    @Override
    @Transactional
    public void deleteById(String itemId) {
        repository.findByExternalId(itemId).ifPresent(repository::delete);
    }

    private CartItem toDomain(CartItemJpaEntity entity) {
        return new CartItem(entity.getExternalId(), entity.getCustomerId(), entity.getProductId(),
                entity.getQuantity());
    }
}
