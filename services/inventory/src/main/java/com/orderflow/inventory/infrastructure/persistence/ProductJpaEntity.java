package com.orderflow.inventory.infrastructure.persistence;

import com.orderflow.inventory.domain.model.ProductStatus;
import com.orderflow.inventory.domain.model.ProductUnit;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "product")
public class ProductJpaEntity {
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 30)
    private String name;

    @Column(name = "description", length = 300)
    private String description;

    @Column(name = "quantity_in_stock", nullable = false, precision = 19, scale = 3)
    private BigDecimal quantityInStock;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit", nullable = false, length = 15)
    private ProductUnit unit;

    @Column(name = "price_per_unit", nullable = false, precision = 15, scale = 2)
    private BigDecimal pricePerUnit;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 15)
    private ProductStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ProductJpaEntity() {
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getQuantityInStock() { return quantityInStock; }
    public void setQuantityInStock(BigDecimal quantityInStock) { this.quantityInStock = quantityInStock; }
    public ProductUnit getUnit() { return unit; }
    public void setUnit(ProductUnit unit) { this.unit = unit; }
    public BigDecimal getPricePerUnit() { return pricePerUnit; }
    public void setPricePerUnit(BigDecimal pricePerUnit) { this.pricePerUnit = pricePerUnit; }
    public ProductStatus getStatus() { return status; }
    public void setStatus(ProductStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
