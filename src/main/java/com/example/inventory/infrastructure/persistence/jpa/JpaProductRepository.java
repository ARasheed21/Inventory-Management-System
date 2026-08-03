package com.example.inventory.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.example.inventory.domain.entities.Product;
import com.example.inventory.domain.repositories.ProductRepository;

@Repository
public class JpaProductRepository implements ProductRepository {
    private final ProductJpaEntityRepository productJpaEntityRepository;
    private final ProductJpaMapper productJpaMapper;

    public JpaProductRepository(ProductJpaEntityRepository productJpaEntityRepository) {
        this.productJpaEntityRepository = productJpaEntityRepository;
        this.productJpaMapper = new ProductJpaMapper();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Product> findById(String id) {
        return productJpaEntityRepository.findByExternalId(id).map(productJpaMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> findAll() {
        return productJpaEntityRepository.findAll().stream().map(productJpaMapper::toDomain).toList();
    }

    @Override
    @Transactional
    public Product save(Product product) {
        ProductJpaEntity entity = productJpaEntityRepository.findByExternalId(product.getId())
                .orElseGet(() -> productJpaMapper.toEntity(product));

        entity.setExternalId(product.getId());
        entity.setName(product.getName());
        entity.setDescription(product.getDescription());
        entity.setPrice(product.getPrice().amount());
        entity.setCurrency(product.getPrice().currency());
        entity.setQuantityInStock(product.getQuantityInStock());
        entity.setVersion(product.getVersion());

        ProductJpaEntity savedEntity = productJpaEntityRepository.save(entity);
        return productJpaMapper.toDomain(savedEntity);
    }
}
