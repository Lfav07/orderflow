package com.orderflow.inventory.domain.repository;

import com.orderflow.inventory.domain.model.StockOperation;
import com.orderflow.inventory.domain.model.StockOperationType;

import java.util.List;
import java.util.UUID;

public interface StockOperationRepository {
    StockOperation save(StockOperation operation);

    StockOperation findById(UUID operationId);

    List<StockOperation> findAllByFilters(UUID productId, StockOperationType type, int page, int size);
}
