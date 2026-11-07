package com.orderflow.inventory.infrastructure.persistence;

import com.orderflow.inventory.domain.model.StockOperation;
import com.orderflow.inventory.domain.repository.StockOperationRepository;
import org.springframework.stereotype.Repository;

@Repository
public class StockOperationRepositoryAdapter implements StockOperationRepository {
    private final StockOperationJpaRepository stockOperationJpaRepository;

    public StockOperationRepositoryAdapter(StockOperationJpaRepository stockOperationJpaRepository) {
        this.stockOperationJpaRepository = stockOperationJpaRepository;
    }

    @Override
    public StockOperation save(StockOperation operation) {
        StockOperationJpaEntity entity = StockOperationJpaMapper.toJpaEntity(operation);
        StockOperationJpaEntity savedEntity = stockOperationJpaRepository.save(entity);
        return StockOperationJpaMapper.toDomainEntity(savedEntity);
    }
}
