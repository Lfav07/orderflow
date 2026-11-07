package com.orderflow.inventory.domain.exception;

import java.util.UUID;

public class StockOperationNotFoundException extends RuntimeException {
    public StockOperationNotFoundException(UUID operationId) {
        super("Stock operation not found: " + operationId);
    }
}
