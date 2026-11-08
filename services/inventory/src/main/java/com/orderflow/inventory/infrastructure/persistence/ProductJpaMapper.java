package com.orderflow.inventory.infrastructure.persistence;

import com.orderflow.inventory.domain.model.Product;

public class ProductJpaMapper {
    public static ProductJpaEntity toProductJpaEntity(Product product) {
        ProductJpaEntity entity = new ProductJpaEntity();
        entity.setId(product.getId());
        entity.setVersion(product.getVersion());
        entity.setName(product.getName());
        entity.setDescription(product.getDescription());
        entity.setPricePerUnit(product.getPricePerUnit());
        entity.setQuantityInStock(product.getQuantityInStock());
        entity.setUnit(product.getUnit());
        entity.setStatus(product.getStatus());
        entity.setCreatedAt(product.getCreatedAt());
        entity.setUpdatedAt(product.getUpdatedAt());
        return entity;
    }

    public static Product toDomainEntity(ProductJpaEntity entity) {
        if (entity == null) return null;

        return Product.rehydrate(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getQuantityInStock(),
                entity.getUnit(),
                entity.getPricePerUnit(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion()
        );
    }
}
