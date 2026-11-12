package com.orderflow.order.domain.model;

public enum OrderStatus {
    WAITING_PAYMENT,
    WAITING_DELIVERY,
    IN_DELIVERY,
    DELIVERED,
    FAILED,
    CANCELLED
}
