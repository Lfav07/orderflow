package com.orderflow.inventory.domain.repository;

import com.orderflow.inventory.domain.model.StockOperation;

public interface StockOperationRepository {
    StockOperation save(StockOperation operation);
}
