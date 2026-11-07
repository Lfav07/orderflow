package com.orderflow.inventory.application.command;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record GetStockOperationByIdCommand(@NotNull UUID operationId) {
}
