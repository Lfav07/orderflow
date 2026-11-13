package com.orderflow.order.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static com.orderflow.order.domain.model.OrderStatus.*;

import static org.junit.jupiter.api.Assertions.*;

class OrderTest {
    @Test
    void progressesThroughSuccessfulDelivery() {
        UUID orderId = UUID.randomUUID();
        Order order = new Order(orderId, List.of(new OrderItem(orderId, UUID.randomUUID(), BigDecimal.ONE, BigDecimal.TEN)));

        order.markWaitingDelivery();
        assertEquals(OrderStatus.WAITING_DELIVERY, order.getStatus());
        order.startDelivery();
        assertEquals(OrderStatus.IN_DELIVERY, order.getStatus());
        order.deliver();
        assertEquals(OrderStatus.DELIVERED, order.getStatus());
    }

    @Test
    void sumsMultipleItemsAndInitializesNewOrder() {
        UUID orderId = UUID.randomUUID();
        OrderItem first = new OrderItem(orderId, UUID.randomUUID(), new BigDecimal("2"), new BigDecimal("3.50"));
        OrderItem second = new OrderItem(orderId, UUID.randomUUID(), new BigDecimal("0.5"), new BigDecimal("4.00"));
        Instant before = Instant.now();
        Order order = new Order(orderId, List.of(first, second));
        Instant after = Instant.now();

        assertNotNull(order.getId());
        assertEquals(OrderStatus.WAITING_PAYMENT, order.getStatus());
        assertEquals(0, new BigDecimal("9.00").compareTo(order.getTotalAmount()));
        assertEquals(List.of(first, second), order.getItems());
        assertNull(order.getVersion());
        assertFalse(order.getCreatedAt().isBefore(before));
        assertFalse(order.getCreatedAt().isAfter(after));
        assertEquals(order.getCreatedAt(), order.getUpdatedAt());
    }

    @Test
    void rejectsNullAndEmptyOrderFields() {
        UUID id = UUID.randomUUID();
        OrderItem item = new OrderItem(id, UUID.randomUUID(), BigDecimal.ONE, BigDecimal.ONE);
        Instant now = Instant.now();
        List<OrderItem> itemsWithNull = new ArrayList<>();
        itemsWithNull.add(null);

        assertThrows(NullPointerException.class, () -> new Order(id, null));
        assertThrows(IllegalArgumentException.class, () -> new Order(UUID.randomUUID(), List.of()));
        assertThrows(NullPointerException.class, () -> new Order(id, itemsWithNull));
        assertThrows(NullPointerException.class, () -> Order.rehydrate(null, OrderStatus.WAITING_PAYMENT, List.of(item), null, now, now));
        assertThrows(NullPointerException.class, () -> Order.rehydrate(id, null, List.of(item), null, now, now));
        assertThrows(NullPointerException.class, () -> Order.rehydrate(id, OrderStatus.WAITING_PAYMENT, null, null, now, now));
        assertThrows(IllegalArgumentException.class, () -> Order.rehydrate(id, OrderStatus.WAITING_PAYMENT, List.of(), null, now, now));
        assertThrows(NullPointerException.class, () -> Order.rehydrate(id, OrderStatus.WAITING_PAYMENT, List.of(item), null, null, now));
        assertThrows(NullPointerException.class, () -> Order.rehydrate(id, OrderStatus.WAITING_PAYMENT, List.of(item), null, now, null));
    }

    private static final Instant OLD_TIMESTAMP = Instant.parse("2025-01-01T00:00:00Z");

    @ParameterizedTest(name = "{0} from {1} transitions to {2}")
    @MethodSource("validTransitions")
    void validTransitionsUpdateStatusAndTimestamp(String name, OrderStatus initial,
                                                   OrderStatus expected, Consumer<Order> action) {
        Order order = orderWithStatus(initial);
        Instant before = Instant.now();
        action.accept(order);
        Instant after = Instant.now();

        assertEquals(expected, order.getStatus());
        assertFalse(order.getUpdatedAt().isBefore(before));
        assertFalse(order.getUpdatedAt().isAfter(after));
    }

    private static Stream<Arguments> validTransitions() {
        return Stream.of(
                Arguments.of("markWaitingDelivery", WAITING_PAYMENT, WAITING_DELIVERY, (Consumer<Order>) Order::markWaitingDelivery),
                Arguments.of("startDelivery", WAITING_DELIVERY, IN_DELIVERY, (Consumer<Order>) Order::startDelivery),
                Arguments.of("deliver", IN_DELIVERY, DELIVERED, (Consumer<Order>) Order::deliver),
                Arguments.of("cancel", WAITING_PAYMENT, CANCELLED, (Consumer<Order>) Order::cancel),
                Arguments.of("fail", WAITING_PAYMENT, FAILED, (Consumer<Order>) Order::fail),
                Arguments.of("fail", WAITING_DELIVERY, FAILED, (Consumer<Order>) Order::fail),
                Arguments.of("fail", IN_DELIVERY, FAILED, (Consumer<Order>) Order::fail)
        );
    }

    @ParameterizedTest(name = "{0} from {1} is rejected")
    @MethodSource("invalidTransitions")
    void invalidTransitionsLeaveStatusAndTimestampUnchanged(String name, OrderStatus initial,
                                                            Consumer<Order> action) {
        Order order = orderWithStatus(initial);

        assertThrows(IllegalStateException.class, () -> action.accept(order));
        assertEquals(initial, order.getStatus());
        assertEquals(OLD_TIMESTAMP, order.getUpdatedAt());
    }

    private static Stream<Arguments> invalidTransitions() {
        return Stream.of(
                invalid("markWaitingDelivery", Order::markWaitingDelivery, WAITING_DELIVERY, IN_DELIVERY, DELIVERED, FAILED, CANCELLED),
                invalid("startDelivery", Order::startDelivery, WAITING_PAYMENT, IN_DELIVERY, DELIVERED, FAILED, CANCELLED),
                invalid("deliver", Order::deliver, WAITING_PAYMENT, WAITING_DELIVERY, DELIVERED, FAILED, CANCELLED),
                invalid("cancel", Order::cancel, WAITING_DELIVERY, IN_DELIVERY, DELIVERED, FAILED, CANCELLED),
                invalid("fail", Order::fail, DELIVERED, FAILED, CANCELLED)
        ).flatMap(stream -> stream);
    }

    private static Stream<Arguments> invalid(String name, Consumer<Order> action, OrderStatus... statuses) {
        return Stream.of(statuses).map(status -> Arguments.of(name, status, action));
    }

    private static Order orderWithStatus(OrderStatus status) {
        UUID orderId = UUID.randomUUID();
        OrderItem item = new OrderItem(orderId, UUID.randomUUID(), BigDecimal.ONE, BigDecimal.ONE);
        return Order.rehydrate(orderId, status, List.of(item), null, OLD_TIMESTAMP, OLD_TIMESTAMP);
    }

    @Test
    void calculatesTotalsAndProtectsItems() {
        UUID orderId = UUID.randomUUID();
        OrderItem item = new OrderItem(orderId, UUID.randomUUID(), new BigDecimal("1.5"), new BigDecimal("2.50"));
        List<OrderItem> source = new ArrayList<>(List.of(item));
        Order order = new Order(orderId, source);
        source.clear();

        assertEquals(0, new BigDecimal("3.750").compareTo(item.getTotalAmount()));
        assertEquals(0, new BigDecimal("3.750").compareTo(order.getTotalAmount()));
        assertEquals(1, order.getItems().size());
        assertThrows(UnsupportedOperationException.class, () -> order.getItems().clear());
        assertEquals(OrderStatus.WAITING_PAYMENT, order.getStatus());
        assertEquals(order.getCreatedAt(), order.getUpdatedAt());
        assertNull(order.getVersion());
    }

    @Test
    void rehydratesIdentityAndPersistenceMetadata() {
        UUID itemId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        Instant created = Instant.parse("2025-01-01T00:00:00Z");
        Instant updated = created.plusSeconds(1);
        OrderItem item = OrderItem.rehydrate(itemId, orderId, UUID.randomUUID(), BigDecimal.ONE, BigDecimal.TEN, created);
        Order order = Order.rehydrate(orderId, OrderStatus.DELIVERED, List.of(item), 2L, created, updated);

        assertEquals(itemId, item.getId());
        assertEquals(orderId, item.getOrderId());
        assertEquals(created, item.getCreatedAt());
        assertEquals(orderId, order.getId());
        assertEquals(OrderStatus.DELIVERED, order.getStatus());
        assertEquals(2L, order.getVersion());
        assertEquals(created, order.getCreatedAt());
        assertEquals(updated, order.getUpdatedAt());
        assertEquals(0, BigDecimal.TEN.compareTo(order.getTotalAmount()));
    }

    @Test
    void rejectsInvalidItemsAndEmptyOrders() {
        UUID productId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class, () -> new OrderItem(orderId, productId, BigDecimal.ZERO, BigDecimal.ONE));
        assertThrows(IllegalArgumentException.class, () -> new OrderItem(orderId, productId, BigDecimal.ONE, BigDecimal.ONE.negate()));
        assertThrows(IllegalArgumentException.class, () -> new Order(UUID.randomUUID(), List.of()));
        assertThrows(NullPointerException.class, () -> new OrderItem(orderId, null, BigDecimal.ONE, BigDecimal.ONE));
        assertThrows(NullPointerException.class, () -> new OrderItem(null, productId, BigDecimal.ONE, BigDecimal.ONE));
        OrderItem item = new OrderItem(orderId, productId, BigDecimal.ONE, BigDecimal.ONE);
        assertThrows(IllegalArgumentException.class, () -> new Order(UUID.randomUUID(), List.of(item)));
        assertThrows(IllegalArgumentException.class, () -> Order.rehydrate(UUID.randomUUID(), WAITING_PAYMENT, List.of(item), null, Instant.now(), Instant.now()));
    }
}
