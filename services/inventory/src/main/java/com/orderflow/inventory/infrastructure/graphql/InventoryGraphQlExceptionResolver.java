package com.orderflow.inventory.infrastructure.graphql;

import com.orderflow.inventory.domain.exception.InsufficientStockException;
import com.orderflow.inventory.domain.exception.LengthExceededException;
import com.orderflow.inventory.domain.exception.NegativePriceException;
import com.orderflow.inventory.domain.exception.ProductNotFoundException;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class InventoryGraphQlExceptionResolver extends DataFetcherExceptionResolverAdapter {
    private static final Logger log = LoggerFactory.getLogger(InventoryGraphQlExceptionResolver.class);

    @Override
    protected GraphQLError resolveToSingleError(Throwable exception, DataFetchingEnvironment environment) {
        if (exception instanceof ProductNotFoundException) {
            return error(environment, ErrorType.NOT_FOUND, "NOT_FOUND", "Product was not found.");
        }
        if (exception instanceof InsufficientStockException) {
            return error(environment, ErrorType.BAD_REQUEST, "CONFLICT", "The requested stock change cannot be applied.");
        }
        if (exception instanceof NegativePriceException
                || exception instanceof LengthExceededException
                || exception instanceof IllegalArgumentException
                || exception instanceof ConstraintViolationException) {
            return error(environment, ErrorType.BAD_REQUEST, "BAD_REQUEST", "The request contains invalid values.");
        }

        log.error("Unexpected inventory GraphQL failure", exception);
        return error(environment, ErrorType.INTERNAL_ERROR, "INTERNAL_ERROR", "An internal error occurred.");
    }

    private GraphQLError error(DataFetchingEnvironment environment, ErrorType errorType, String code, String message) {
        return GraphqlErrorBuilder.newError(environment)
                .errorType(errorType)
                .message(message)
                .extensions(Map.of("code", code))
                .build();
    }
}
