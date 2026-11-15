package com.orderflow.order.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
        this.quantity = normalizeQuantity(quantity);
        this.unitPrice = exactScale(unitPrice, 2, 15, "Unit price");
        if (this.unitPrice.signum() < 0)
            throw new IllegalArgumentException("Unit price cannot be negative");
        this.totalAmount = this.quantity.multiply(this.unitPrice).setScale(2, RoundingMode.HALF_UP);
        if (this.totalAmount.precision() > 15)
            throw new IllegalArgumentException("Item total exceeds supported precision");
        this.createdAt = Objects.requireNonNull(createdAt, "Created at cannot be null");
    }

    public static BigDecimal normalizeQuantity(BigDecimal quantity) {
        BigDecimal scaled = exactScale(quantity, 3, 19, "Quantity");
        if (scaled.signum() <= 0)
            throw new IllegalArgumentException("Quantity must be greater than zero");
        return scaled;
    }

    private static BigDecimal exactScale(BigDecimal value, int scale, int precision, String name) {
        Objects.requireNonNull(value, name + " cannot be null");
        final BigDecimal scaled;
        try {
            scaled = value.setScale(scale, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(name + " exceeds supported scale of " + scale, exception);
        }
        if (scaled.precision() > precision)
            throw new IllegalArgumentException(name + " exceeds supported precision");
        return scaled;
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
