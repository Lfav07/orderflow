package com.orderflow.inventory.application.command;

import com.orderflow.inventory.domain.model.ProductStatus;
import com.orderflow.inventory.domain.model.ProductUnit;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateProductCommand(
        @NotBlank
        @Size(max = 30)
        String name,

        @Size(max = 300)
        String description,

        @NotNull
        @DecimalMin(value = "0.0", inclusive = true)
        BigDecimal quantityInStock,

        @NotNull
        ProductUnit unit,

        @NotNull
        @DecimalMin(value = "0.0", inclusive = true)
        BigDecimal pricePerUnit,

        @NotNull
        ProductStatus status
) {
}
