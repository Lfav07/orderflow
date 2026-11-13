package com.orderflow.order.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Order {
    private final UUID id;
    private OrderStatus status;
    private final List<OrderItem> items;
    private final Long version;
    private final BigDecimal totalAmount;
    private final Instant createdAt;
    private  Instant updatedAt;

    public Order(UUID id, List<OrderItem> items) {
        this(id, OrderStatus.WAITING_PAYMENT, items, null, Instant.now());
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
        if (this.items.stream().anyMatch(item -> !this.id.equals(item.getOrderId())))
            throw new IllegalArgumentException("All items must belong to the order");
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

    public void markWaitingDelivery() {
        transitionFrom(OrderStatus.WAITING_PAYMENT, OrderStatus.WAITING_DELIVERY);
    }

    public void startDelivery() {
        transitionFrom(OrderStatus.WAITING_DELIVERY, OrderStatus.IN_DELIVERY);
    }

    public void deliver() {
        transitionFrom(OrderStatus.IN_DELIVERY, OrderStatus.DELIVERED);
    }

    public void cancel() {
        transitionFrom(OrderStatus.WAITING_PAYMENT, OrderStatus.CANCELLED);
    }

    public void fail() {
        if (status == OrderStatus.DELIVERED ||
                status == OrderStatus.CANCELLED ||
                status == OrderStatus.FAILED) {
            throw new IllegalStateException(
                    "Cannot fail order from " + status + " to FAILED"
            );
        }
        this.status = OrderStatus.FAILED;
        updateTimestamp();
    }

    private void transitionFrom(OrderStatus expected, OrderStatus next) {
        if (status != expected)
            throw new IllegalStateException("Cannot transition order from " + status + " to " + next);
        this.status = next;
        updateTimestamp();
    }

    private void updateTimestamp() {
        this.updatedAt = Instant.now();
    }


    public UUID getId() { return id; }
    public OrderStatus getStatus() { return status; }
    public List<OrderItem> getItems() { return items; }
    public Long getVersion() { return version; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
