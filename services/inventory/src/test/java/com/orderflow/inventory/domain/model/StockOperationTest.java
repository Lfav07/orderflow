package com.orderflow.inventory.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class StockOperationTest {
    @Test
    void createsOperationWithRequiredValues() {
        UUID operationId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        BigDecimal quantity = new BigDecimal("2.50");

        StockOperation operation = new StockOperation(operationId, productId, quantity, StockOperationType.DEBIT);

        assertNotNull(operation);
        assertEquals(operationId, operation.getOperationId());
        assertEquals(productId, operation.getProductId());
        assertEquals(quantity, operation.getQuantity());
        assertEquals(StockOperationType.DEBIT, operation.getType());
        assertNotNull(operation.getCreatedAt());
    }

    @Test
    void rejectsNullRequiredValues() {
        UUID operationId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        assertThrows(NullPointerException.class,
                () -> new StockOperation(null, productId, BigDecimal.ONE, StockOperationType.DEBIT));
        assertThrows(NullPointerException.class,
                () -> new StockOperation(operationId, null, BigDecimal.ONE, StockOperationType.DEBIT));
        assertThrows(NullPointerException.class,
                () -> new StockOperation(operationId, productId, null, StockOperationType.DEBIT));
        assertThrows(NullPointerException.class,
                () -> new StockOperation(operationId, productId, BigDecimal.ONE, null));
    }

    @Test
    void rejectsZeroOrNegativeQuantity() {
        UUID operationId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        assertThrows(IllegalArgumentException.class,
                () -> new StockOperation(operationId, productId, BigDecimal.ZERO, StockOperationType.RETURN));
        assertThrows(IllegalArgumentException.class,
                () -> new StockOperation(operationId, productId, new BigDecimal("-0.01"), StockOperationType.RETURN));
    }

    @Test
    void enforcesQuantityPrecisionAndScaleBoundaries() {
        UUID operationId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        StockOperation atMaximumPrecision = new StockOperation(operationId, productId,
                new BigDecimal("9999999999999999.999"), StockOperationType.ADJUSTMENT_IN);

        assertEquals(new BigDecimal("9999999999999999.999"), atMaximumPrecision.getQuantity());
        assertThrows(IllegalArgumentException.class, () -> new StockOperation(UUID.randomUUID(), productId,
                new BigDecimal("10000000000000000"), StockOperationType.ADJUSTMENT_IN));
        assertThrows(IllegalArgumentException.class, () -> new StockOperation(UUID.randomUUID(), productId,
                new BigDecimal("1.0001"), StockOperationType.ADJUSTMENT_IN));
    }

    @Test
    void rehydratesOperationWithoutChangingItsIdentityOrTimestamp() {
        UUID operationId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2025-01-02T03:04:05Z");

        StockOperation operation = StockOperation.rehydrate(operationId, productId,
                new BigDecimal("2.500"), StockOperationType.RETURN, createdAt);

        assertEquals(operationId, operation.getOperationId());
        assertEquals(productId, operation.getProductId());
        assertEquals(new BigDecimal("2.500"), operation.getQuantity());
        assertEquals(StockOperationType.RETURN, operation.getType());
        assertEquals(createdAt, operation.getCreatedAt());
    }

    @Test
    void rejectsMissingTimestampDuringRehydration() {
        assertThrows(NullPointerException.class, () -> StockOperation.rehydrate(UUID.randomUUID(),
                UUID.randomUUID(), BigDecimal.ONE, StockOperationType.DEBIT, null));
    }
}
