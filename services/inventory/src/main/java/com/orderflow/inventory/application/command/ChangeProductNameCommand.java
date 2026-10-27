package com.orderflow.inventory.application.command;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record ChangeProductNameCommand(
        @NotNull UUID id,
        @NotNull @Size(min = 1, max = 30) String name
) {}
