package com.orderflow.inventory.infrastructure.graphql;

import com.orderflow.inventory.domain.repository.ProductRepository;
import com.orderflow.inventory.testsupport.PostgresTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.graphql.test.autoconfigure.tester.AutoConfigureHttpGraphQlTester;
import org.springframework.graphql.test.tester.HttpGraphQlTester;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureHttpGraphQlTester
class ProductGraphQlIT extends PostgresTestSupport {
    @Autowired HttpGraphQlTester graphQlTester;
    @Autowired ProductRepository products;

    @Test
    void createProductMutationPersistsAndReturnsProductFields() {
        String uniqueName = "GQL-" + java.util.UUID.randomUUID().toString().substring(0, 12);

        String id = graphQlTester.document("""
                        mutation Create($input: CreateProductInput!) {
                          createProduct(input: $input) {
                            id name description quantityInStock unit pricePerUnit status
                          }
                        }
                        """)
                .variable("input", Map.of("name", uniqueName, "description", "Beans", "quantityInStock", 2.5,
                        "unit", "KILOGRAMS", "pricePerUnit", 4.75, "status", "ACTIVE"))
                .execute()
                .errors().verify()
                .path("createProduct.name").entity(String.class).isEqualTo(uniqueName)
                .path("createProduct.id").entity(String.class).get();

        var stored = products.findById(java.util.UUID.fromString(id));
        assertEquals("Beans", stored.getDescription());
        assertEquals("2.5", stored.getQuantityInStock().stripTrailingZeros().toPlainString());
    }

    @Test
    void listProductsQueryReturnsSeededProducts() {
        var product = products.save(new com.orderflow.inventory.domain.model.Product("LIST-" + java.util.UUID.randomUUID().toString().substring(0, 12),
                null, java.math.BigDecimal.ONE, com.orderflow.inventory.domain.model.ProductUnit.UNITS,
                java.math.BigDecimal.ONE, com.orderflow.inventory.domain.model.ProductStatus.ACTIVE));

        graphQlTester.document("""
                        query List($input: ListProductsInput!) {
                          listProducts(input: $input) { id name quantityInStock }
                        }
                        """)
                .variable("input", Map.of("page", 0, "size", 100))
                .execute()
                .errors().verify()
                .path("listProducts[*].id").entityList(String.class).contains(product.getId().toString());
    }
}
