package com.orderflow.order.infrastructure.graphql;

import com.orderflow.order.application.gateway.InsufficientStockException;
import com.orderflow.order.domain.exception.InvalidOrderTransitionException;
import com.orderflow.order.domain.exception.OrderNotFoundException;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindException;

import java.util.Map;

@Component
public class OrderGraphQlExceptionResolver extends DataFetcherExceptionResolverAdapter {
    private static final Logger log = LoggerFactory.getLogger(OrderGraphQlExceptionResolver.class);

    @Override
    protected GraphQLError resolveToSingleError(Throwable exception, DataFetchingEnvironment environment) {
        // A failed compensation may leave remote stock inconsistent; do not disguise it as a client error.
        if (exception.getSuppressed().length > 0) {
            return unexpected(exception, environment);
        }
        if (exception instanceof OrderNotFoundException) {
            return error(environment, ErrorType.NOT_FOUND, "NOT_FOUND", "Order was not found.");
        }
        if (exception instanceof InvalidOrderTransitionException || exception instanceof OptimisticLockingFailureException) {
            return error(environment, ErrorType.BAD_REQUEST, "CONFLICT", "The order transition cannot be applied.");
        }
        if (exception instanceof InsufficientStockException) {
            return error(environment, ErrorType.BAD_REQUEST, "CONFLICT", "Insufficient stock for the order.");
        }
        if (exception instanceof IllegalArgumentException || exception instanceof BindException) {
            return error(environment, ErrorType.BAD_REQUEST, "BAD_REQUEST", "The request contains invalid values.");
        }
        if (exception instanceof StatusRuntimeException || exception instanceof StatusException) {
            Status.Code code = Status.fromThrowable(exception).getCode();
            if (code == Status.Code.NOT_FOUND) {
                return error(environment, ErrorType.NOT_FOUND, "NOT_FOUND", "Product was not found.");
            }
            if (code == Status.Code.FAILED_PRECONDITION || code == Status.Code.ALREADY_EXISTS) {
                return error(environment, ErrorType.BAD_REQUEST, "CONFLICT", "The requested stock change cannot be applied.");
            }
            // Inventory INVALID_ARGUMENT can signal an invalid internal request, not necessarily client input.
            log.error("Inventory gRPC call failed with status {}", code, exception);
            return error(environment, ErrorType.INTERNAL_ERROR, "UPSTREAM_ERROR", "Inventory service request failed.");
        }
        return unexpected(exception, environment);
    }

    private GraphQLError unexpected(Throwable exception, DataFetchingEnvironment environment) {
        log.error("Unexpected order GraphQL failure", exception);
        return error(environment, ErrorType.INTERNAL_ERROR, "INTERNAL_ERROR", "An internal error occurred.");
    }

    private GraphQLError error(DataFetchingEnvironment environment, ErrorType type, String code, String message) {
        return GraphqlErrorBuilder.newError(environment).errorType(type).message(message)
                .extensions(Map.of("code", code)).build();
    }
}
