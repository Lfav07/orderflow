package com.orderflow.inventory.application.command;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record ChangeProductDescriptionCommand(
        @NotNull UUID id,
        @Size(max = 300) String description
) {}
