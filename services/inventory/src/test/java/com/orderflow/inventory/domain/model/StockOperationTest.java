package com.orderflow.inventory.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
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
}
