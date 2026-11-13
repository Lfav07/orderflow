package com.orderflow.order.application.command;

import com.orderflow.order.domain.model.OrderStatus;

public record FilterOrdersCommand(OrderStatus status, int page, int size) {
    public FilterOrdersCommand(OrderStatus status) { this(status, 0, 20); }
}
