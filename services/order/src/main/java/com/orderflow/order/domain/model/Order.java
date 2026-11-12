package com.orderflow.order.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Order {
    private final UUID id;
    private final OrderStatus status;
    private final List<OrderItem> items;
    private final Long version;
    private final BigDecimal totalAmount;
    private final Instant createdAt;
    private final Instant updatedAt;

    public Order(List<OrderItem> items) {
        this(UUID.randomUUID(), OrderStatus.WAITING_PAYMENT, items, null, Instant.now());
    }

    private Order(UUID id, OrderStatus status, List<OrderItem> items, Long version, Instant now) {
        this(id, status, items, version, now, now);
    }

    private Order(UUID id, OrderStatus status, List<OrderItem> items, Long version,
                  Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "Order id cannot be null");
        this.status = Objects.requireNonNull(status, "Status cannot be null");
        this.items = List.copyOf(Objects.requireNonNull(items, "Items cannot be null"));
        if (this.items.isEmpty())
            throw new IllegalArgumentException("Order must contain at least one item");
        this.version = version;
        this.totalAmount = this.items.stream().map(OrderItem::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.createdAt = Objects.requireNonNull(createdAt, "Created at cannot be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "Updated at cannot be null");
    }

    public static Order rehydrate(UUID id, OrderStatus status, List<OrderItem> items, Long version,
                                  Instant createdAt, Instant updatedAt) {
        return new Order(id, status, items, version, createdAt, updatedAt);
    }

    public UUID getId() { return id; }
    public OrderStatus getStatus() { return status; }
    public List<OrderItem> getItems() { return items; }
    public Long getVersion() { return version; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
