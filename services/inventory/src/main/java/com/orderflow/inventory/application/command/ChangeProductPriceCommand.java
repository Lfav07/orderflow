package com.orderflow.inventory.application.command;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record ChangeProductPriceCommand(
        @NotNull UUID id,
        @NotNull @DecimalMin("0.0") BigDecimal pricePerUnit
) {}
