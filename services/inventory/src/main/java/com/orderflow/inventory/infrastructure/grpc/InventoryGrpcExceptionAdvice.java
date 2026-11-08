package com.orderflow.inventory.infrastructure.grpc;

import com.orderflow.inventory.domain.exception.InsufficientStockException;
import com.orderflow.inventory.domain.exception.LengthExceededException;
import com.orderflow.inventory.domain.exception.NegativePriceException;
import com.orderflow.inventory.domain.exception.ProductNotFoundException;
import io.grpc.Status;
import jakarta.validation.ConstraintViolationException;
import org.springframework.grpc.server.advice.GrpcAdvice;
import org.springframework.grpc.server.advice.GrpcExceptionHandler;

@GrpcAdvice
public class InventoryGrpcExceptionAdvice {
    @GrpcExceptionHandler(ProductNotFoundException.class)
    public Status productNotFound(ProductNotFoundException exception) {
        return Status.NOT_FOUND.withDescription("Product was not found.");
    }

    @GrpcExceptionHandler(InsufficientStockException.class)
    public Status insufficientStock(InsufficientStockException exception) {
        return Status.FAILED_PRECONDITION.withDescription("The requested stock change cannot be applied.");
    }

    @GrpcExceptionHandler({NegativePriceException.class, LengthExceededException.class,
            IllegalArgumentException.class, ConstraintViolationException.class})
    public Status invalidRequest(RuntimeException exception) {
        return Status.INVALID_ARGUMENT.withDescription("The request contains invalid values.");
    }

    @GrpcExceptionHandler(Throwable.class)
    public Status unexpectedFailure(Throwable exception) {
        return Status.INTERNAL.withDescription("An internal error occurred.");
    }
}
