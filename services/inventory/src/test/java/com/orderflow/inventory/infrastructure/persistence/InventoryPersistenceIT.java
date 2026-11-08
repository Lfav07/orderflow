package com.orderflow.inventory.infrastructure.persistence;

import com.orderflow.inventory.domain.exception.ProductNotFoundException;
import com.orderflow.inventory.domain.exception.StockOperationNotFoundException;
import com.orderflow.inventory.domain.model.*;
import com.orderflow.inventory.domain.repository.ProductRepository;
import com.orderflow.inventory.domain.repository.StockOperationRepository;
import com.orderflow.inventory.testsupport.PostgresTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** PostgreSQL-backed adapter and migration checks. Requires a Docker-compatible runtime. */
@SpringBootTest
class InventoryPersistenceIT extends PostgresTestSupport {

    @Autowired ProductRepository products;
    @Autowired StockOperationRepository operations;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManagerFactory entityManagerFactory;

    @Test
    void productPersistsAndReloadsAllDomainValues() {
        Product created = products.save(new Product("Coffee-" + UUID.randomUUID().toString().substring(0, 8), null,
                new BigDecimal("3.250"), ProductUnit.KILOGRAMS, new BigDecimal("12.50"), ProductStatus.ACTIVE));

        Product loaded = products.findById(created.getId());

        assertEquals(created.getId(), loaded.getId());
        assertEquals(created.getName(), loaded.getName());
        assertNull(loaded.getDescription());
        assertEquals(new BigDecimal("3.250"), loaded.getQuantityInStock());
        assertEquals(ProductUnit.KILOGRAMS, loaded.getUnit());
        assertEquals(new BigDecimal("12.50"), loaded.getPricePerUnit());
        assertEquals(ProductStatus.ACTIVE, loaded.getStatus());
        assertNotNull(loaded.getCreatedAt());
        assertNotNull(loaded.getVersion());
    }

    @Test
    void stockOperationPersistsAndCanBeFilteredByProductAndType() {
        UUID productId = UUID.randomUUID();
        StockOperation created = operations.save(new StockOperation(UUID.randomUUID(), productId,
                new BigDecimal("1.125"), StockOperationType.RETURN));

        assertEquals(created.getOperationId(), operations.findById(created.getOperationId()).getOperationId());
        assertEquals(1, operations.findAllByFilters(productId, StockOperationType.RETURN, 0, 10).size());
        assertEquals(0, operations.findAllByFilters(productId, StockOperationType.DEBIT, 0, 10).size());
    }

    @Test
    void adaptersTranslateMissingIdsToDomainNotFoundExceptions() {
        assertThrows(ProductNotFoundException.class, () -> products.findById(UUID.randomUUID()));
        assertThrows(StockOperationNotFoundException.class, () -> operations.findById(UUID.randomUUID()));
    }

    @Test
    void productQueriesApplyNameSortAndPageBoundaries() {
        String token = "PAGE-" + UUID.randomUUID().toString().substring(0, 8);
        Product middle = saveProduct(token + "-B", ProductStatus.ACTIVE, ProductUnit.UNITS);
        Product first = saveProduct(token + "-A", ProductStatus.ACTIVE, ProductUnit.UNITS);
        Product last = saveProduct(token + "-C", ProductStatus.ACTIVE, ProductUnit.UNITS);

        var firstPage = products.findByFilters(ProductStatus.ACTIVE, token, ProductUnit.UNITS, 0, 2);
        var secondPage = products.findByFilters(ProductStatus.ACTIVE, token, ProductUnit.UNITS, 1, 2);
        var emptyPage = products.findByFilters(ProductStatus.ACTIVE, token, ProductUnit.UNITS, 2, 2);

        assertEquals(java.util.List.of(first.getId(), middle.getId()), firstPage.stream().map(Product::getId).toList());
        assertEquals(java.util.List.of(last.getId()), secondPage.stream().map(Product::getId).toList());
        assertTrue(emptyPage.isEmpty());
    }

    @Test
    void stockOperationQueriesSortNewestFirstAndApplyPages() {
        UUID productId = UUID.randomUUID();
        StockOperation oldest = saveOperation(productId, StockOperationType.RETURN);
        StockOperation middle = saveOperation(productId, StockOperationType.RETURN);
        StockOperation newest = saveOperation(productId, StockOperationType.RETURN);
        jdbc.update("update stock_operation set created_at = ? where operation_id = ?",
                java.sql.Timestamp.from(java.time.Instant.parse("2026-01-01T00:00:00Z")), oldest.getOperationId());
        jdbc.update("update stock_operation set created_at = ? where operation_id = ?",
                java.sql.Timestamp.from(java.time.Instant.parse("2026-01-02T00:00:00Z")), middle.getOperationId());
        jdbc.update("update stock_operation set created_at = ? where operation_id = ?",
                java.sql.Timestamp.from(java.time.Instant.parse("2026-01-03T00:00:00Z")), newest.getOperationId());

        var firstPage = operations.findAllByFilters(productId, StockOperationType.RETURN, 0, 2);
        var secondPage = operations.findAllByFilters(productId, StockOperationType.RETURN, 1, 2);

        assertEquals(java.util.List.of(newest.getOperationId(), middle.getOperationId()),
                firstPage.stream().map(StockOperation::getOperationId).toList());
        assertEquals(java.util.List.of(oldest.getOperationId()),
                secondPage.stream().map(StockOperation::getOperationId).toList());
    }

    @Test
    void databaseConstraintsRejectInvalidProductAndOperationRows() {
        Product product = saveProduct("CHECK-" + UUID.randomUUID().toString().substring(0, 8),
                ProductStatus.ACTIVE, ProductUnit.UNITS);

        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("update product set quantity_in_stock = -1 where id = ?", product.getId()));
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("update product set unit = 'INVALID' where id = ?", product.getId()));
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("update product set name = null where id = ?", product.getId()));

        StockOperation operation = saveOperation(UUID.randomUUID(), StockOperationType.DEBIT);
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("update stock_operation set quantity = 0 where operation_id = ?", operation.getOperationId()));
    }

    @Test
    void staleProductUpdateIsRejectedByOptimisticVersioning() {
        Product saved = saveProduct("VERSION-" + UUID.randomUUID().toString().substring(0, 8),
                ProductStatus.ACTIVE, ProductUnit.UNITS);
        EntityManager staleManager = entityManagerFactory.createEntityManager();
        EntityManager currentManager = entityManagerFactory.createEntityManager();
        EntityTransaction staleTx = staleManager.getTransaction();
        EntityTransaction currentTx = currentManager.getTransaction();
        try {
            staleTx.begin();
            ProductJpaEntity stale = staleManager.find(ProductJpaEntity.class, saved.getId());

            currentTx.begin();
            ProductJpaEntity current = currentManager.find(ProductJpaEntity.class, saved.getId());
            current.setName("CURRENT-" + UUID.randomUUID().toString().substring(0, 8));
            currentTx.commit();

            stale.setName("STALE-" + UUID.randomUUID().toString().substring(0, 8));
            assertThrows(jakarta.persistence.OptimisticLockException.class, staleManager::flush);
        } finally {
            if (staleTx.isActive()) staleTx.rollback();
            if (currentTx.isActive()) currentTx.rollback();
            staleManager.close();
            currentManager.close();
        }
        assertTrue(products.findById(saved.getId()).getName().startsWith("CURRENT-"));
    }

    private Product saveProduct(String name, ProductStatus status, ProductUnit unit) {
        return products.save(new Product(name, null, BigDecimal.ONE, unit, BigDecimal.ONE, status));
    }

    private StockOperation saveOperation(UUID productId, StockOperationType type) {
        return operations.save(new StockOperation(UUID.randomUUID(), productId, new BigDecimal("1.000"), type));
    }

    @Test
    void flywayAppliedAllInventoryMigrationsIncludingOptimisticVersionColumn() {
        assertEquals(3, jdbc.queryForObject("select count(*) from flyway_schema_history where success = true", Integer.class));
        assertEquals(1, jdbc.queryForObject("select count(*) from information_schema.columns where table_name = 'product' and column_name = 'version'", Integer.class));
        assertEquals(1, jdbc.queryForObject("select count(*) from information_schema.tables where table_name = 'stock_operation'", Integer.class));
    }
}
