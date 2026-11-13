package com.orderflow.order.application.usecase;

import com.orderflow.order.application.command.TransitionOrderCommand;
import com.orderflow.order.domain.model.Order;
import com.orderflow.order.domain.repository.OrderRepository;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;

final class OrderTransition {
    private OrderTransition() {}

    static Order execute(OrderRepository repository, TransitionOrderCommand command, Consumer<Order> transition) {
        Objects.requireNonNull(command, "Command cannot be null");
        var id = Objects.requireNonNull(command.id(), "Order id cannot be null");
        Order order = repository.findById(id);
        if (order == null) {
            throw new NoSuchElementException("Order not found: " + id);
        }
        transition.accept(order);
        return repository.save(order);
    }
}
