package com.orderflow.order.application.command;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemCommand(
   UUID productId,
   BigDecimal quantity
) {
}
