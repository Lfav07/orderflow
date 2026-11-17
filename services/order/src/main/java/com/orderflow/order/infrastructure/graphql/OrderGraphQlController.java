package com.orderflow.order.infrastructure.graphql;

import com.orderflow.order.application.command.*;
import com.orderflow.order.application.usecase.*;
import com.orderflow.order.domain.model.Order;
import com.orderflow.order.domain.model.OrderItem;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Controller
public class OrderGraphQlController {
    private final GetOrderByIdUseCase getOrderById;
    private final ListOrdersUseCase listOrders;
    private final FilterOrdersUseCase filterOrders;
    private final CreateOrderUseCase createOrder;
    private final MarkOrderWaitingDeliveryUseCase markWaitingDelivery;
    private final StartOrderDeliveryUseCase startDelivery;
    private final DeliverOrderUseCase deliver;
    private final CancelOrderUseCase cancel;
    private final FailOrderUseCase fail;

    public OrderGraphQlController(GetOrderByIdUseCase getOrderById, ListOrdersUseCase listOrders,
            FilterOrdersUseCase filterOrders, CreateOrderUseCase createOrder,
            MarkOrderWaitingDeliveryUseCase markWaitingDelivery, StartOrderDeliveryUseCase startDelivery,
            DeliverOrderUseCase deliver, CancelOrderUseCase cancel, FailOrderUseCase fail) {
        this.getOrderById = getOrderById;
        this.listOrders = listOrders;
        this.filterOrders = filterOrders;
        this.createOrder = createOrder;
        this.markWaitingDelivery = markWaitingDelivery;
        this.startDelivery = startDelivery;
        this.deliver = deliver;
        this.cancel = cancel;
        this.fail = fail;
    }

    @QueryMapping
    public Order orderById(@Argument("input") GetOrderByIdCommand input) { return getOrderById.execute(input); }

    @QueryMapping
    public List<Order> listOrders(@Argument("input") ListOrdersCommand input) { return listOrders.execute(input); }

    @QueryMapping
    public List<Order> filterOrders(@Argument("input") FilterOrdersCommand input) { return filterOrders.execute(input); }

    @MutationMapping
    public Order createOrder(@Argument("input") CreateOrderCommand input) { return createOrder.execute(input); }

    @MutationMapping
    public Order markOrderWaitingDelivery(@Argument("input") TransitionOrderCommand input) { return markWaitingDelivery.execute(input); }

    @MutationMapping
    public Order startOrderDelivery(@Argument("input") TransitionOrderCommand input) { return startDelivery.execute(input); }

    @MutationMapping
    public Order deliverOrder(@Argument("input") TransitionOrderCommand input) { return deliver.execute(input); }

    @MutationMapping
    public Order cancelOrder(@Argument("input") TransitionOrderCommand input) { return cancel.execute(input); }

    @MutationMapping
    public Order failOrder(@Argument("input") TransitionOrderCommand input) { return fail.execute(input); }

    @SchemaMapping(typeName = "Order", field = "createdAt")
    public OffsetDateTime createdAt(Order order) { return order.getCreatedAt().atOffset(ZoneOffset.UTC); }

    @SchemaMapping(typeName = "Order", field = "updatedAt")
    public OffsetDateTime updatedAt(Order order) { return order.getUpdatedAt().atOffset(ZoneOffset.UTC); }

    @SchemaMapping(typeName = "OrderItem", field = "createdAt")
    public OffsetDateTime itemCreatedAt(OrderItem item) { return item.getCreatedAt().atOffset(ZoneOffset.UTC); }
}
