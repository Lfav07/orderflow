package com.orderflow.inventory.infrastructure.persistence;

import com.orderflow.inventory.domain.model.StockOperation;
import com.orderflow.inventory.domain.model.StockOperationType;
import com.orderflow.inventory.domain.exception.StockOperationNotFoundException;
import com.orderflow.inventory.domain.repository.StockOperationRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

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

    @Override
    public StockOperation findById(UUID operationId) {
        return stockOperationJpaRepository.findById(operationId)
                .map(StockOperationJpaMapper::toDomainEntity)
                .orElseThrow(() -> new StockOperationNotFoundException(operationId));
    }

    @Override
    public List<StockOperation> findAllByFilters(UUID productId, StockOperationType type, int page, int size) {
        return stockOperationJpaRepository.findAllByFilters(
                        productId,
                        type,
                        PageRequest.of(page, size, Sort.by("createdAt").descending()))
                .map(StockOperationJpaMapper::toDomainEntity)
                .getContent();
    }
}
