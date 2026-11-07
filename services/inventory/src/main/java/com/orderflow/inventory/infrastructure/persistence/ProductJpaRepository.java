package com.orderflow.inventory.infrastructure.persistence;

import com.orderflow.inventory.domain.model.ProductStatus;
import com.orderflow.inventory.domain.model.ProductUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProductJpaRepository extends JpaRepository<ProductJpaEntity, UUID> {
    @Query("""
            SELECT p FROM ProductJpaEntity p
            WHERE (:status IS NULL OR p.status = :status)
              AND (:unit IS NULL OR p.unit = :unit)
              AND (:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')))
            """)
    Page<ProductJpaEntity> findByFilters(@Param("status") ProductStatus status,
                                         @Param("unit") ProductUnit unit,
                                         @Param("name") String name,
                                         Pageable pageable);
}
