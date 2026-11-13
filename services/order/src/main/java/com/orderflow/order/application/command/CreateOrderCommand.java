package com.orderflow.order.application.command;

import java.util.List;

public record CreateOrderCommand(
        List<OrderItemCommand> items
) {
}
