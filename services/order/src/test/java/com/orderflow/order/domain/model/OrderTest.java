package com.orderflow.order.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OrderTest {
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
