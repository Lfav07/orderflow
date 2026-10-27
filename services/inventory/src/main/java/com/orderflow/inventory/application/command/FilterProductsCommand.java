package com.orderflow.inventory.application.command;

import com.orderflow.inventory.domain.model.ProductStatus;
import com.orderflow.inventory.domain.model.ProductUnit;
import jakarta.validation.constraints.Min;

public record FilterProductsCommand(
        ProductStatus status,
        String name,
        ProductUnit unit,
        @Min(0) int page,
        @Min(1) int size
) {
    public FilterProductsCommand(ProductStatus status, String name, ProductUnit unit) {
        this(status, name, unit, 0, 20);
    }
}
