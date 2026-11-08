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

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** PostgreSQL-backed adapter and migration checks. Requires a Docker-compatible runtime. */
@SpringBootTest
class InventoryPersistenceIT extends PostgresTestSupport {

    @Autowired ProductRepository products;
    @Autowired StockOperationRepository operations;
    @Autowired JdbcTemplate jdbc;

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
    void flywayAppliedAllInventoryMigrationsIncludingOptimisticVersionColumn() {
        assertEquals(3, jdbc.queryForObject("select count(*) from flyway_schema_history where success = true", Integer.class));
        assertEquals(1, jdbc.queryForObject("select count(*) from information_schema.columns where table_name = 'product' and column_name = 'version'", Integer.class));
        assertEquals(1, jdbc.queryForObject("select count(*) from information_schema.tables where table_name = 'stock_operation'", Integer.class));
    }
}
