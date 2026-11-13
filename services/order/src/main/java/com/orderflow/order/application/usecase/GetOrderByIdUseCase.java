package com.orderflow.order.application.usecase;

import com.orderflow.order.application.command.GetOrderByIdCommand;
import com.orderflow.order.domain.model.Order;
import com.orderflow.order.domain.repository.OrderRepository;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class GetOrderByIdUseCase {
    private final OrderRepository repository;

    public GetOrderByIdUseCase(OrderRepository repository) {
        this.repository = Objects.requireNonNull(repository, "Order repository cannot be null");
    }

    public Order execute(GetOrderByIdCommand command) {
        Objects.requireNonNull(command, "Get order command cannot be null");
        return repository.findById(Objects.requireNonNull(command.id(), "Order id cannot be null"));
    }
}
