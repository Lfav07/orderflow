package com.orderflow.inventory.infrastructure.persistence;

import com.orderflow.inventory.domain.model.StockOperation;

public class StockOperationJpaMapper {

    public static StockOperationJpaEntity toJpaEntity(StockOperation operation) {
        if (operation == null) return null;

        StockOperationJpaEntity entity = new StockOperationJpaEntity();
        entity.setOperationId(operation.getOperationId());
        entity.setProductId(operation.getProductId());
        entity.setQuantity(operation.getQuantity());
        entity.setType(operation.getType());
        entity.setCreatedAt(operation.getCreatedAt());
        return entity;
    }

    public static StockOperation toDomainEntity(StockOperationJpaEntity entity) {
        if (entity == null) return null;

        return StockOperation.rehydrate(
                entity.getOperationId(),
                entity.getProductId(),
                entity.getQuantity(),
                entity.getType(),
                entity.getCreatedAt()
        );
    }
}
