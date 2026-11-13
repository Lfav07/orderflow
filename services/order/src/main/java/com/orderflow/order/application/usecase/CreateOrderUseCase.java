package com.orderflow.order.application.usecase;

import com.orderflow.order.application.command.CreateOrderItemCommand;
import com.orderflow.order.application.command.CreateOrderCommand;
import com.orderflow.order.application.command.OrderItemCommand;
import com.orderflow.order.domain.model.Order;
import com.orderflow.order.domain.model.OrderItem;
import com.orderflow.order.domain.repository.OrderItemRepository;
import com.orderflow.order.domain.repository.OrderRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component
public class CreateOrderUseCase {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CreateOrderItemUseCase createOrderItemUseCase;

    public CreateOrderUseCase(OrderRepository orderRepository, OrderItemRepository orderItemRepository, CreateOrderItemUseCase createOrderItemUseCase) {
        this.orderRepository = Objects.requireNonNull(orderRepository, "Order repository cannot be null");
        this.orderItemRepository = Objects.requireNonNull(orderItemRepository, "Order item repository cannot be null");
        this.createOrderItemUseCase = Objects.requireNonNull(createOrderItemUseCase, "Create order item use case cannot be null");
    }

    @Transactional
    public Order execute(CreateOrderCommand command) {
        Objects.requireNonNull(command, "Command cannot be null");
        List<OrderItemCommand> items = List.copyOf(Objects.requireNonNull(command.items(), "Items cannot be null"));
        if (items.isEmpty()) throw new IllegalArgumentException("Order must contain at least one item");
        for (OrderItemCommand item : items) {
            Objects.requireNonNull(item.productId(), "Product id cannot be null");
            Objects.requireNonNull(item.quantity(), "Quantity cannot be null");
            if (item.quantity().signum() <= 0) throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        UUID orderId = UUID.randomUUID();
        List<OrderItem> orderItems = createItems(items, orderId);
        Order savedOrder = orderRepository.save(new Order(orderId, orderItems));
        orderItemRepository.saveAll(orderItems);
        return savedOrder;
    }

    private List<OrderItem> createItems(List<OrderItemCommand> commands, UUID orderId) {
        List<OrderItem> res = new ArrayList<>();
        for (OrderItemCommand command : commands) {
            CreateOrderItemCommand createOrderItemCommand = new CreateOrderItemCommand(orderId, command.productId(), command.quantity());
            OrderItem orderItem = createOrderItemUseCase.execute(createOrderItemCommand);
            res.add(orderItem);
        }
        return res;
    }
}
