package com.orderflow.order.application.usecase;

import com.orderflow.order.application.command.TransitionOrderCommand;
import com.orderflow.order.domain.model.Order;
import com.orderflow.order.domain.repository.OrderRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Component
public class StartOrderDeliveryUseCase {
    private final OrderRepository repository;

    public StartOrderDeliveryUseCase(OrderRepository repository) {
        this.repository = Objects.requireNonNull(repository, "Order repository cannot be null");
    }

    @Transactional
    public Order execute(TransitionOrderCommand command) {
        return OrderTransition.execute(repository, command, Order::startDelivery);
    }
}
