package com.example.inventory.infrastructure.persistence.jpa;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.envers.Audited;

@Entity
@Audited
@Table(name = "products")
public class ProductJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", nullable = false, unique = true)
    private String externalId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "price", nullable = false)
    private BigDecimal price;

    @Column(name = "currency", nullable = false)
    private String currency;

    @Column(name = "quantity_in_stock", nullable = false)
    private Integer quantityInStock;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "category")
    private String category;

    protected ProductJpaEntity() {
    }

    public ProductJpaEntity(String externalId, String name, String description, BigDecimal price, String currency,
            Integer quantityInStock, Long version) {
        this(externalId, name, description, price, currency, quantityInStock, version, null);
    }

    public ProductJpaEntity(String externalId, String name, String description, BigDecimal price, String currency,
            Integer quantityInStock, Long version, String category) {
        this.externalId = externalId;
        this.name = name;
        this.description = description;
        this.price = price;
        this.currency = currency;
        this.quantityInStock = quantityInStock;
        this.version = version;
        this.category = category;
    }

    public Long getId() {
        return id;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Integer getQuantityInStock() {
        return quantityInStock;
    }

    public void setQuantityInStock(Integer quantityInStock) {
        this.quantityInStock = quantityInStock;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
