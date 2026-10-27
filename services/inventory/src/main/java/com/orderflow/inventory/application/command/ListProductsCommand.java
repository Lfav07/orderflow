package com.orderflow.inventory.application.command;

import jakarta.validation.constraints.Min;

public record ListProductsCommand(
        @Min(0) int page,
        @Min(1) int size
) {
    public ListProductsCommand() {
        this(0, 20);
    }
}
