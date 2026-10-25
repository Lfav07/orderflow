package com.orderflow.inventory.domain.exception;

public class LengthExceededException extends RuntimeException {
    public LengthExceededException(String message) {
        super(message);
    }
}
