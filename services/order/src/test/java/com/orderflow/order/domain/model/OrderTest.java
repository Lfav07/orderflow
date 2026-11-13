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
        Order order = new Order(List.of(new OrderItem(UUID.randomUUID(), BigDecimal.ONE, BigDecimal.TEN)));

        order.markWaitingDelivery();
        assertEquals(OrderStatus.WAITING_DELIVERY, order.getStatus());
        order.startDelivery();
        assertEquals(OrderStatus.IN_DELIVERY, order.getStatus());
        order.deliver();
        assertEquals(OrderStatus.DELIVERED, order.getStatus());
    }

    @Test
    void sumsMultipleItemsAndInitializesNewOrder() {
        OrderItem first = new OrderItem(UUID.randomUUID(), new BigDecimal("2"), new BigDecimal("3.50"));
        OrderItem second = new OrderItem(UUID.randomUUID(), new BigDecimal("0.5"), new BigDecimal("4.00"));
        Instant before = Instant.now();
        Order order = new Order(List.of(first, second));
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
        OrderItem item = new OrderItem(UUID.randomUUID(), BigDecimal.ONE, BigDecimal.ONE);
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        List<OrderItem> itemsWithNull = new ArrayList<>();
        itemsWithNull.add(null);

        assertThrows(NullPointerException.class, () -> new Order(null));
        assertThrows(IllegalArgumentException.class, () -> new Order(List.of()));
        assertThrows(NullPointerException.class, () -> new Order(itemsWithNull));
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
        OrderItem item = new OrderItem(UUID.randomUUID(), BigDecimal.ONE, BigDecimal.ONE);
        return Order.rehydrate(UUID.randomUUID(), status, List.of(item), null, OLD_TIMESTAMP, OLD_TIMESTAMP);
    }

    @Test
    void calculatesTotalsAndProtectsItems() {
        OrderItem item = new OrderItem(UUID.randomUUID(), new BigDecimal("1.5"), new BigDecimal("2.50"));
        List<OrderItem> source = new ArrayList<>(List.of(item));
        Order order = new Order(source);
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
        OrderItem item = OrderItem.rehydrate(itemId, UUID.randomUUID(), BigDecimal.ONE, BigDecimal.TEN, created);
        Order order = Order.rehydrate(orderId, OrderStatus.DELIVERED, List.of(item), 2L, created, updated);

        assertEquals(itemId, item.getId());
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
        assertThrows(IllegalArgumentException.class, () -> new OrderItem(productId, BigDecimal.ZERO, BigDecimal.ONE));
        assertThrows(IllegalArgumentException.class, () -> new OrderItem(productId, BigDecimal.ONE, BigDecimal.ONE.negate()));
        assertThrows(IllegalArgumentException.class, () -> new Order(List.of()));
        assertThrows(NullPointerException.class, () -> new OrderItem(null, BigDecimal.ONE, BigDecimal.ONE));
    }
}
