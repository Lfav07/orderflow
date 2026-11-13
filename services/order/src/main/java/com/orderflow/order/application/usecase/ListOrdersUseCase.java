package com.orderflow.order.application.usecase;

import com.orderflow.order.application.command.ListOrdersCommand;
import com.orderflow.order.domain.model.Order;
import com.orderflow.order.domain.repository.OrderRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class ListOrdersUseCase {
    public static final int MAX_PAGE_SIZE = 100;
    private final OrderRepository repository;

    public ListOrdersUseCase(OrderRepository repository) {
        this.repository = Objects.requireNonNull(repository, "Order repository cannot be null");
    }

    public List<Order> execute(ListOrdersCommand command) {
        Objects.requireNonNull(command, "List orders command cannot be null");
        validatePage(command.page(), command.size());
        return repository.findAll(command.page(), command.size());
    }

    static void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Page must be non-negative and size must be between 1 and " + MAX_PAGE_SIZE);
        }
    }
}
