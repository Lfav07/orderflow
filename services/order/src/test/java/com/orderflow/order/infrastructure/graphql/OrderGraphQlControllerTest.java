package com.orderflow.order.infrastructure.graphql;

import com.orderflow.order.application.command.*;
import com.orderflow.order.application.usecase.*;
import com.orderflow.order.domain.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@GraphQlTest(OrderGraphQlController.class)
@Import({GraphQlConfig.class, OrderGraphQlExceptionResolver.class})
class OrderGraphQlControllerTest {
    @Autowired GraphQlTester graphQl;
    @MockitoBean GetOrderByIdUseCase getById;
    @MockitoBean ListOrdersUseCase list;
    @MockitoBean FilterOrdersUseCase filter;
    @MockitoBean CreateOrderUseCase create;
    @MockitoBean MarkOrderWaitingDeliveryUseCase waiting;
    @MockitoBean StartOrderDeliveryUseCase start;
    @MockitoBean DeliverOrderUseCase deliver;
    @MockitoBean CancelOrderUseCase cancel;
    @MockitoBean FailOrderUseCase fail;

    @Test
    void createBindsNestedInputAndSerializesOrderFields() {
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        OrderItem item = new OrderItem(orderId, productId, new BigDecimal("2.125"), new BigDecimal("4.50"));
        Order order = new Order(orderId, List.of(item));
        when(create.execute(any())).thenReturn(order);

        graphQl.document("""
                mutation Create($input: CreateOrderInput!) {
                  createOrder(input: $input) {
                    id status version totalAmount createdAt updatedAt
                    items { id orderId productId quantity unitPrice totalAmount createdAt }
                  }
                }
                """).variable("input", Map.of("items", List.of(Map.of("productId", productId.toString(), "quantity", 2.125))))
                .execute().errors().verify()
                .path("createOrder.id").entity(String.class).isEqualTo(orderId.toString())
                .path("createOrder.status").entity(String.class).isEqualTo("WAITING_PAYMENT")
                .path("createOrder.items[0].productId").entity(String.class).isEqualTo(productId.toString())
                .path("createOrder.createdAt").entity(String.class).satisfies(value -> assertTrue(value.endsWith("Z")))
                .path("createOrder.items[0].createdAt").entity(String.class).satisfies(value -> assertTrue(value.endsWith("Z")));
        var captor = org.mockito.ArgumentCaptor.forClass(CreateOrderCommand.class);
        verify(create).execute(captor.capture());
        assertEquals(productId, captor.getValue().items().getFirst().productId());
        assertEquals(0, new BigDecimal("2.125").compareTo(captor.getValue().items().getFirst().quantity()));
    }

    @Test
    void queryDefaultsAndTransitionBindCommands() {
        when(list.execute(any())).thenReturn(List.of());
        when(filter.execute(any())).thenReturn(List.of());
        graphQl.document("{ listOrders(input: {}) { id } filterOrders(input: {status: DELIVERED}) { id } }")
                .execute().errors().verify();
        verify(list).execute(new ListOrdersCommand(0, 20));
        verify(filter).execute(new FilterOrdersCommand(OrderStatus.DELIVERED, 0, 20));

        UUID id = UUID.randomUUID();
        Order order = new Order(id, List.of(new OrderItem(id, UUID.randomUUID(), BigDecimal.ONE, BigDecimal.ONE)));
        when(cancel.execute(any())).thenReturn(order);
        graphQl.document("mutation($input: OrderIdInput!) { cancelOrder(input: $input) { id } }")
                .variable("input", Map.of("id", id.toString())).execute().errors().verify();
        verify(cancel).execute(new TransitionOrderCommand(id));
    }
    @Test
    void expectedFailuresHaveStableCodesAndSafeMessages() {
        doThrow(new IllegalArgumentException("private detail")).when(create).execute(any());
        assertError("mutation { createOrder(input:{items:[]}) { id } }", "BAD_REQUEST", "createOrder");

        doThrow(io.grpc.Status.NOT_FOUND.withDescription("private detail").asRuntimeException()).when(create).execute(any());
        assertError("mutation { createOrder(input:{items:[]}) { id } }", "NOT_FOUND", "createOrder");

        doThrow(new com.orderflow.order.application.gateway.InsufficientStockException("private detail")).when(create).execute(any());
        assertError("mutation { createOrder(input:{items:[]}) { id } }", "CONFLICT", "createOrder");

        doThrow(new com.orderflow.order.domain.exception.InvalidOrderTransitionException("private detail")).when(cancel).execute(any());
        assertError("mutation { cancelOrder(input:{id:\"00000000-0000-0000-0000-000000000000\"}) { id } }", "CONFLICT", "cancelOrder");

        doThrow(new com.orderflow.order.domain.exception.OrderNotFoundException(UUID.randomUUID())).when(cancel).execute(any());
        assertError("mutation { cancelOrder(input:{id:\"00000000-0000-0000-0000-000000000000\"}) { id } }", "NOT_FOUND", "cancelOrder");

        doThrow(io.grpc.Status.UNAVAILABLE.withDescription("private detail").asRuntimeException()).when(create).execute(any());
        assertError("mutation { createOrder(input:{items:[]}) { id } }", "UPSTREAM_ERROR", "createOrder");

        doThrow(new RuntimeException("private detail")).when(create).execute(any());
        assertError("mutation { createOrder(input:{items:[]}) { id } }", "INTERNAL_ERROR", "createOrder");
    }

    @Test
    void invalidUuidIsReportedAsBadRequest() {
        assertError("mutation { cancelOrder(input:{id:\"not-a-uuid\"}) { id } }", "BAD_REQUEST", "cancelOrder");
    }

    private void assertError(String document, String code, String path) {
        graphQl.document(document).execute().errors().satisfy(errors -> {
            assertEquals(1, errors.size());
            assertEquals(code, errors.getFirst().getExtensions().get("code"));
            assertEquals(path, errors.getFirst().getPath());
            assertFalse(errors.getFirst().getMessage().contains("private detail"));
        });
    }

}
