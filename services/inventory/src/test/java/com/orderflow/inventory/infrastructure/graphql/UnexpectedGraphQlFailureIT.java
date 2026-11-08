package com.orderflow.inventory.infrastructure.graphql;

import com.orderflow.inventory.domain.model.Product;
import com.orderflow.inventory.domain.model.ProductStatus;
import com.orderflow.inventory.domain.model.ProductUnit;
import com.orderflow.inventory.domain.repository.ProductRepository;
import com.orderflow.inventory.testsupport.PostgresTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.graphql.test.autoconfigure.tester.AutoConfigureHttpGraphQlTester;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.graphql.test.tester.GraphQlTester;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureHttpGraphQlTester
@Import(UnexpectedGraphQlFailureIT.FailingProductRepositoryConfiguration.class)
class UnexpectedGraphQlFailureIT extends PostgresTestSupport {
    private static final String SENTINEL = "SENSITIVE_SENTINEL_DATABASE_DETAIL";
    private static final UUID TRIGGER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Autowired GraphQlTester graphQlTester;

    @Test
    void unexpectedResolverFailureIsInternalAndDoesNotExposeExceptionDetails() {
        var response = graphQlTester.document("""
                        query Product($input: GetProductByIdInput!) {
                          productById(input: $input) { id }
                        }
                        """)
                .variable("input", java.util.Map.of("id", TRIGGER_ID.toString()))
                .execute();

        response.errors().satisfy(errors -> {
            assertFalse(errors.isEmpty());
            var error = errors.get(0);
            assertEquals("INTERNAL_ERROR", error.getExtensions().get("code"));
            assertNotNull(error.getMessage());
            assertFalse(error.getMessage().isBlank());
            assertFalse(error.getMessage().contains(SENTINEL));
            assertFalse(String.valueOf(error.getExtensions()).contains(SENTINEL));
            assertNotNull(error.getPath());
        });
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FailingProductRepositoryConfiguration {
        @Bean
        @Primary
        ProductRepository failingProductRepository() {
            return new ProductRepository() {
                @Override
                public Product save(Product product) {
                    throw new UnsupportedOperationException("Not used by this test");
                }

                @Override
                public Product findById(UUID id) {
                    throw new IllegalStateException(SENTINEL);
                }

                @Override
                public List<Product> findAll(int page, int size) {
                    return List.of();
                }

                @Override
                public List<Product> findByFilters(ProductStatus status, String name, ProductUnit unit,
                                                   int page, int size) {
                    return List.of();
                }
            };
        }
    }
}
