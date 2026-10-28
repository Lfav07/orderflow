package com.orderflow.inventory.application.command;

import com.orderflow.inventory.domain.model.StockOperationType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record StockOperationCommand(
        @NotNull UUID operationId,
        @NotNull UUID productId,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal quantity,
        @NotNull StockOperationType type
) {}
