package com.orderflow.order.domain.exception;

import java.util.NoSuchElementException;
import java.util.UUID;

public class OrderNotFoundException extends NoSuchElementException {
    public OrderNotFoundException(UUID id) {
        super("Order not found: " + id);
    }
}
