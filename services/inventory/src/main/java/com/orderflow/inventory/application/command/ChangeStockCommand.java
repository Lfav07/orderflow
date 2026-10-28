package com.orderflow.inventory.application.command;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record ChangeStockCommand(
        @NotNull UUID id,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal quantity
) {}
