package com.orderflow.inventory.application.command;

import com.orderflow.inventory.domain.model.ProductUnit;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ChangeProductUnitCommand(@NotNull UUID id, @NotNull ProductUnit unit) {}
