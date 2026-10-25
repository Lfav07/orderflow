package com.orderflow.inventory.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class StockOperation {
    private final UUID operationId;
    private final UUID productId;
    private final BigDecimal quantity;
    private final StockOperationType type;
    private final Instant createdAt;

    public StockOperation(UUID operationId, UUID productId, BigDecimal quantity, StockOperationType type) {
        this.operationId = Objects.requireNonNull(operationId, "Operation id cannot be null");
        this.productId = Objects.requireNonNull(productId, "Product id cannot be null");
        this.quantity = validateQuantity(quantity);
        this.type = Objects.requireNonNull(type, "Operation type cannot be null");
        this.createdAt = Instant.now();
    }

    private static BigDecimal validateQuantity(BigDecimal quantity) {
        Objects.requireNonNull(quantity, "Quantity cannot be null");
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Quantity must be greater than zero"
            );
        }
        return quantity;
    }
}