package com.orderflow.order.application.command;

import java.util.UUID;

public record GetOrderByIdCommand(UUID id) {}
