package com.orderflow.inventory.application.command;

import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.UUID;

public record GetProductByIdCommand(
        @NotNull UUID id) {
}
