package com.example.inventory.web;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.inventory.application.ResourceNotFoundException;
import com.example.inventory.infrastructure.persistence.jpa.AuditRevisionEntity;
import com.example.inventory.infrastructure.persistence.jpa.OrderJpaEntity;
import com.example.inventory.infrastructure.persistence.jpa.OrderJpaEntityRepository;
import com.example.inventory.infrastructure.persistence.jpa.ProductJpaEntity;
import com.example.inventory.infrastructure.persistence.jpa.ProductJpaEntityRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@RestController
@RequestMapping("/api/admin/audit")
@Tag(name = "Audit", description = "Admin audit-history endpoints")
public class AuditHistoryController {

    @PersistenceContext
    private EntityManager entityManager;

    private final ProductJpaEntityRepository productJpaEntityRepository;
    private final OrderJpaEntityRepository orderJpaEntityRepository;

    public AuditHistoryController(ProductJpaEntityRepository productJpaEntityRepository,
            OrderJpaEntityRepository orderJpaEntityRepository) {
        this.productJpaEntityRepository = productJpaEntityRepository;
        this.orderJpaEntityRepository = orderJpaEntityRepository;
    }

    public record AuditEntryResponse(
            long revision,
            long timestamp,
            String author,
            String revisionType,
            Map<String, Object> snapshot) {
    }

    @GetMapping("/products/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Product change history", description = "Returns the Envers revision history for a product.")
    @ApiResponse(responseCode = "200", description = "Audit history returned")
    public ResponseEntity<List<AuditEntryResponse>> productHistory(@PathVariable String id) {
        ProductJpaEntity product = productJpaEntityRepository.findByExternalId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
        return ResponseEntity.ok(readHistory(ProductJpaEntity.class, product.getId()));
    }

    @GetMapping("/orders/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Order change history", description = "Returns the Envers revision history for an order.")
    @ApiResponse(responseCode = "200", description = "Audit history returned")
    public ResponseEntity<List<AuditEntryResponse>> orderHistory(@PathVariable String id) {
        OrderJpaEntity order = orderJpaEntityRepository.findByExternalId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
        return ResponseEntity.ok(readHistory(OrderJpaEntity.class, order.getId()));
    }

    @SuppressWarnings("unchecked")
    private List<AuditEntryResponse> readHistory(Class<?> entityType, Long internalId) {
        AuditReader auditReader = AuditReaderFactory.get(entityManager);
        List<Map<String, Object>> rows = new ArrayList<>();
        List<Number> revisions = (List<Number>) (List<?>) auditReader.createQuery()
                .forRevisionsOfEntity(entityType, false, true)
                .add(AuditEntity.id().eq(internalId))
                .getResultList();
        for (Object row : revisions) {
            Object[] tuple = (Object[]) row;
            Object entity = tuple[0];
            AuditRevisionEntity revision = (AuditRevisionEntity) tuple[1];
            RevisionType type = (RevisionType) tuple[2];
            rows.add(Map.of(
                    "entity", entity,
                    "revision", revision,
                    "type", type));
        }
        return rows.stream().map(row -> {
            AuditRevisionEntity revision = (AuditRevisionEntity) row.get("revision");
            return new AuditEntryResponse(
                    revision.getId(),
                    revision.getTimestamp(),
                    revision.getUserName(),
                    ((RevisionType) row.get("type")).name(),
                    snapshot((Object) row.get("entity")));
        }).toList();
    }

    private Map<String, Object> snapshot(Object entity) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        if (entity instanceof ProductJpaEntity product) {
            snapshot.put("id", product.getExternalId());
            snapshot.put("name", product.getName());
            snapshot.put("price", product.getPrice());
            snapshot.put("currency", product.getCurrency());
            snapshot.put("quantityInStock", product.getQuantityInStock());
            snapshot.put("category", product.getCategory());
        } else if (entity instanceof OrderJpaEntity order) {
            snapshot.put("id", order.getExternalId());
            snapshot.put("customerId", order.getCustomerId());
            snapshot.put("status", order.getStatus());
            snapshot.put("totalAmount", order.getTotalAmount());
            snapshot.put("currency", order.getCurrency());
        }
        return snapshot;
    }
}
