package com.orderflow.inventory.application.command;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ProductIdCommand(@NotNull UUID id) {}
