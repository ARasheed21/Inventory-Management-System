package com.example.inventory.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.example.inventory.domain.entities.Product;
import com.example.inventory.domain.repositories.ProductRepository;

import jakarta.persistence.criteria.Predicate;

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
    @Transactional(readOnly = true)
    public SearchResult search(String searchTerm, String category, int page, int size) {
        Specification<ProductJpaEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new java.util.ArrayList<>();
            if (StringUtils.hasText(searchTerm)) {
                String like = "%" + searchTerm.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(root.get("description")), like)));
            }
            if (StringUtils.hasText(category)) {
                predicates.add(cb.equal(cb.lower(root.get("category")), category.toLowerCase()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        var result = productJpaEntityRepository.findAll(spec,
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.ASC, "name")));

        List<Product> products = result.getContent().stream().map(productJpaMapper::toDomain).toList();
        return new SearchResult(products, result.getTotalElements(), result.getNumber(), result.getSize());
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
        entity.setCategory(product.getCategory());

        ProductJpaEntity savedEntity = productJpaEntityRepository.save(entity);
        return productJpaMapper.toDomain(savedEntity);
    }
}
