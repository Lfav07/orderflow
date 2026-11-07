package com.orderflow.inventory.application.command;

import com.orderflow.inventory.domain.model.StockOperationType;
import jakarta.validation.constraints.Min;

import java.util.UUID;

public record ListStockOperationsCommand(
        UUID productId,
        StockOperationType type,
        @Min(0) int page,
        @Min(1) int size
) {
    public ListStockOperationsCommand(UUID productId, StockOperationType type) {
        this(productId, type, 0, 20);
    }

    public ListStockOperationsCommand() {
        this(null, null, 0, 20);
    }
}
