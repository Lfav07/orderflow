package com.orderflow.order.domain.exception;

public class InvalidOrderTransitionException extends IllegalStateException {
    public InvalidOrderTransitionException(String message) {
        super(message);
    }
}
