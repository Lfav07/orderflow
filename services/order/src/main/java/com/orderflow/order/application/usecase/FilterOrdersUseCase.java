package com.orderflow.order.application.usecase;

import com.orderflow.order.application.command.FilterOrdersCommand;
import com.orderflow.order.domain.model.Order;
import com.orderflow.order.domain.repository.OrderRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class FilterOrdersUseCase {
    private final OrderRepository repository;

    public FilterOrdersUseCase(OrderRepository repository) {
        this.repository = Objects.requireNonNull(repository, "Order repository cannot be null");
    }

    public List<Order> execute(FilterOrdersCommand command) {
        Objects.requireNonNull(command, "Filter orders command cannot be null");
        Objects.requireNonNull(command.status(), "Order status cannot be null");
        ListOrdersUseCase.validatePage(command.page(), command.size());
        return repository.findByFilters(command.status(), command.page(), command.size());
    }
}
