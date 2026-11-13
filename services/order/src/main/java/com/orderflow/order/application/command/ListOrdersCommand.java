package com.orderflow.order.application.command;

public record ListOrdersCommand(int page, int size) {
    public ListOrdersCommand() { this(0, 20); }
}
