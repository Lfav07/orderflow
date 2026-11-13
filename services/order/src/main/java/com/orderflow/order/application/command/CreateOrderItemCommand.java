package com.orderflow.order.application.command;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateOrderItemCommand(
        UUID orderId,
        UUID productId,
        BigDecimal quantity
) {
}
