package com.orderflow.inventory.infrastructure.persistence;

import com.orderflow.inventory.domain.model.StockOperationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface StockOperationJpaRepository extends JpaRepository<StockOperationJpaEntity, UUID> {
    @Query("""
            SELECT operation FROM StockOperationJpaEntity operation
            WHERE (:productId IS NULL OR operation.productId = :productId)
              AND (:type IS NULL OR operation.type = :type)
            """)
    Page<StockOperationJpaEntity> findAllByFilters(@Param("productId") UUID productId,
                                                  @Param("type") StockOperationType type,
                                                  Pageable pageable);
}
