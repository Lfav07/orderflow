package com.orderflow.inventory.infrastructure.graphql;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Component
public class InventoryGraphQlRequestErrorInterceptor implements WebGraphQlInterceptor {
    @Override
    public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest request, Chain chain) {
        return chain.next(request).map(response -> {
            List<GraphQLError> errors = response.getExecutionResult().getErrors().stream()
                    .map(this::classifyRequestError)
                    .toList();
            return response.transform(builder -> builder.errors(errors).build());
        });
    }

    private GraphQLError classifyRequestError(GraphQLError error) {
        if (error.getPath() != null) {
            return error;
        }

        var builder = GraphqlErrorBuilder.newError()
                .message("Invalid GraphQL request.")
                .errorType(org.springframework.graphql.execution.ErrorType.BAD_REQUEST)
                .extensions(Map.of("code", "BAD_REQUEST"));
        if (error.getLocations() != null) {
            builder.locations(error.getLocations());
        }
        return builder.build();
    }
}
