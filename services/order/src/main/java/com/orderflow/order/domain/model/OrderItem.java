package com.orderflow.order.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class OrderItem {
    private final UUID id;
    private final UUID orderId;
    private final UUID productId;
    private final BigDecimal quantity;
    private final BigDecimal unitPrice;
    private final BigDecimal totalAmount;
    private final Instant createdAt;

    public OrderItem(UUID orderId, UUID productId, BigDecimal quantity, BigDecimal unitPrice) {
        this(UUID.randomUUID(), orderId, productId, quantity, unitPrice, Instant.now());
    }

    private OrderItem(UUID id, UUID orderId, UUID productId, BigDecimal quantity, BigDecimal unitPrice, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "Item id cannot be null");
        this.orderId = Objects.requireNonNull(orderId, "Order id cannot be null");
        this.productId = Objects.requireNonNull(productId, "Product id cannot be null");
        this.quantity = Objects.requireNonNull(quantity, "Quantity cannot be null");
        this.unitPrice = Objects.requireNonNull(unitPrice, "Unit price cannot be null");
        if (quantity.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Quantity must be greater than zero");
        if (unitPrice.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("Unit price cannot be negative");
        this.totalAmount = quantity.multiply(unitPrice);
        this.createdAt = Objects.requireNonNull(createdAt, "Created at cannot be null");
    }

    public static OrderItem rehydrate(UUID id, UUID orderId, UUID productId, BigDecimal quantity,
                                      BigDecimal unitPrice, Instant createdAt) {
        return new OrderItem(id, orderId, productId, quantity, unitPrice, createdAt);
    }

    public UUID getId() { return id; }
    public UUID getOrderId() { return orderId; }
    public UUID getProductId() { return productId; }
    public BigDecimal getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public Instant getCreatedAt() { return createdAt; }
}
