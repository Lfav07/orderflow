package com.orderflow.inventory.infrastructure.graphql;

import com.orderflow.inventory.domain.repository.ProductRepository;
import com.orderflow.inventory.domain.repository.StockOperationRepository;
import com.orderflow.inventory.domain.model.Product;
import com.orderflow.inventory.domain.model.ProductStatus;
import com.orderflow.inventory.domain.model.ProductUnit;
import com.orderflow.inventory.domain.model.StockOperation;
import com.orderflow.inventory.domain.model.StockOperationType;
import com.orderflow.inventory.testsupport.PostgresTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.graphql.test.autoconfigure.tester.AutoConfigureHttpGraphQlTester;
import org.springframework.graphql.test.tester.HttpGraphQlTester;

import java.util.Map;
import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureHttpGraphQlTester
class ProductGraphQlIT extends PostgresTestSupport {
    @Autowired HttpGraphQlTester graphQlTester;
    @Autowired ProductRepository products;
    @Autowired StockOperationRepository operations;

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

    @Test
    void queriesReturnProductByIdAvailableStockAndFilteredProducts() {
        String marker = "QUERY-" + UUID.randomUUID().toString().substring(0, 8);
        Product matching = saveProduct(marker + "-A", ProductStatus.ACTIVE, ProductUnit.UNITS, "4.250");
        saveProduct(marker + "-B", ProductStatus.INACTIVE, ProductUnit.UNITS, "9");

        var productResponse = graphQlTester.document("""
                        query Product($input: GetProductByIdInput!) {
                          productById(input: $input) { id name quantityInStock unit status createdAt }
                        }
                        """)
                .variable("input", Map.of("id", matching.getId().toString()))
                .execute();
        productResponse.errors().verify();
        assertEquals(matching.getId().toString(), productResponse.path("productById.id").entity(String.class).get());
        assertEquals(0, productResponse.path("productById.quantityInStock").entity(BigDecimal.class).get()
                .compareTo(new BigDecimal("4.250")));
        productResponse.path("productById.createdAt").entity(String.class).get();

        var availableResponse = graphQlTester.document("""
                        query Available($input: ProductIdInput!) { availableStock(input: $input) }
                        """)
                .variable("input", Map.of("id", matching.getId().toString()))
                .execute();
        availableResponse.errors().verify();
        assertEquals(0, availableResponse.path("availableStock").entity(BigDecimal.class).get()
                .compareTo(new BigDecimal("4.250")));

        graphQlTester.document("""
                        query Filter($input: FilterProductsInput!) {
                          filterProducts(input: $input) { id name status }
                        }
                        """)
                .variable("input", Map.of("status", "ACTIVE", "name", marker, "unit", "UNITS", "page", 0, "size", 10))
                .execute().errors().verify()
                .path("filterProducts[*].id").entityList(String.class).contains(matching.getId().toString());
    }

    @Test
    void productMutationsChangeAttributesStockAndStatus() {
        Product product = saveProduct("MUT-" + UUID.randomUUID().toString().substring(0, 8),
                ProductStatus.INACTIVE, ProductUnit.UNITS, "5");
        String id = product.getId().toString();

        executeMutation("""
                mutation($input: ChangeProductNameInput!) { changeProductName(input: $input) { id name } }
                """, "input", Map.of("id", id, "name", "Renamed item"));
        executeMutation("""
                mutation($input: ChangeProductDescriptionInput!) { changeProductDescription(input: $input) { id description } }
                """, "input", Map.of("id", id, "description", "Fresh description"));
        executeMutation("""
                mutation($input: ChangeProductPriceInput!) { changeProductPrice(input: $input) { id pricePerUnit } }
                """, "input", Map.of("id", id, "pricePerUnit", 7.25));
        executeMutation("""
                mutation($input: ChangeProductUnitInput!) { changeProductUnit(input: $input) { id unit } }
                """, "input", Map.of("id", id, "unit", "KILOGRAMS"));
        executeMutation("""
                mutation($input: ChangeStockInput!) { increaseStock(input: $input) { id quantityInStock } }
                """, "input", Map.of("id", id, "quantity", 2.5));
        executeMutation("""
                mutation($input: ChangeStockInput!) { decreaseStock(input: $input) { id quantityInStock } }
                """, "input", Map.of("id", id, "quantity", 1.25));
        executeMutation("""
                mutation($input: ProductIdInput!) { activateProduct(input: $input) { id status } }
                """, "input", Map.of("id", id));

        Product changed = products.findById(product.getId());
        assertEquals("Renamed item", changed.getName());
        assertEquals("Fresh description", changed.getDescription());
        assertEquals(new BigDecimal("7.25"), changed.getPricePerUnit());
        assertEquals(ProductUnit.KILOGRAMS, changed.getUnit());
        assertEquals(0, changed.getQuantityInStock().compareTo(new BigDecimal("6.25")));
        assertEquals(ProductStatus.ACTIVE, changed.getStatus());

        executeMutation("""
                mutation($input: ProductIdInput!) { deactivateProduct(input: $input) { id status } }
                """, "input", Map.of("id", id));
        assertEquals(ProductStatus.INACTIVE, products.findById(product.getId()).getStatus());
    }

    @Test
    void stockOperationQueriesExposePersistedOperationAndOptionalFilters() {
        UUID productId = UUID.randomUUID();
        StockOperation operation = operations.save(new StockOperation(UUID.randomUUID(), productId,
                new BigDecimal("1.250"), StockOperationType.RETURN));

        graphQlTester.document("""
                        query Operation($input: GetStockOperationByIdInput!) {
                          stockOperationById(input: $input) { operationId productId quantity type createdAt }
                        }
                        """)
                .variable("input", Map.of("operationId", operation.getOperationId().toString()))
                .execute().errors().verify()
                .path("stockOperationById.operationId").entity(String.class).isEqualTo(operation.getOperationId().toString())
                .path("stockOperationById.type").entity(String.class).isEqualTo("RETURN");

        graphQlTester.document("""
                        query Operations($input: ListStockOperationsInput!) {
                          listStockOperations(input: $input) { operationId type quantity }
                        }
                        """)
                .variable("input", Map.of("productId", productId.toString(), "type", "RETURN", "page", 0, "size", 10))
                .execute().errors().verify()
                .path("listStockOperations[*].operationId").entityList(String.class).contains(operation.getOperationId().toString());
    }

    @Test
    void requestSyntaxErrorHasBadRequestCodeAndNoExecutionPath() {
        var response = graphQlTester.document("query {").execute();

        assertGraphQlError(response, "BAD_REQUEST", null, true);
    }

    @Test
    void documentValidationErrorHasBadRequestCodeAndNoExecutionPath() {
        var response = graphQlTester.document("query { unknownInventoryField }").execute();

        assertGraphQlError(response, "BAD_REQUEST", null, true);
    }

    @Test
    void variableCoercionErrorHasBadRequestCodeAndNoExecutionPath() {
        var response = graphQlTester.document("""
                query Filter($input: FilterProductsInput!) {
                  filterProducts(input: $input) { id }
                }
                """)
                .variable("input", Map.of("unit", "NOT_A_PRODUCT_UNIT", "page", 0, "size", 10))
                .execute();

        assertGraphQlError(response, "BAD_REQUEST", null, true);
    }

    @Test
    void invalidStockMutationHasBadRequestCodeAndLeavesStockUnchanged() {
        Product product = saveProduct("INVALID-" + UUID.randomUUID().toString().substring(0, 8),
                ProductStatus.ACTIVE, ProductUnit.UNITS, "5");
        var response = graphQlTester.document("""
                mutation($input: ChangeStockInput!) { decreaseStock(input: $input) { id quantityInStock } }
                """)
                .variable("input", Map.of("id", product.getId().toString(), "quantity", 0))
                .execute();

        assertGraphQlError(response, "BAD_REQUEST", "IllegalArgumentException", false);
        assertEquals(0, products.findById(product.getId()).getQuantityInStock().compareTo(new BigDecimal("5")));
    }

    @Test
    void missingProductHasNotFoundCodeAndSafeMessage() {
        var response = graphQlTester.document("""
                query Product($input: GetProductByIdInput!) { productById(input: $input) { id } }
                """)
                .variable("input", Map.of("id", UUID.randomUUID().toString()))
                .execute();

        assertGraphQlError(response, "NOT_FOUND", "ProductNotFoundException", false);
    }

    @Test
    void insufficientStockHasConflictCodeAndLeavesStockUnchanged() {
        Product product = saveProduct("INSUFFICIENT-" + UUID.randomUUID().toString().substring(0, 8),
                ProductStatus.ACTIVE, ProductUnit.UNITS, "1");
        var response = graphQlTester.document("""
                mutation($input: ChangeStockInput!) { decreaseStock(input: $input) { id quantityInStock } }
                """)
                .variable("input", Map.of("id", product.getId().toString(), "quantity", 2))
                .execute();

        assertGraphQlError(response, "CONFLICT", "InsufficientStockException", false);
        assertEquals(0, products.findById(product.getId()).getQuantityInStock().compareTo(BigDecimal.ONE));
    }

    private void assertGraphQlError(org.springframework.graphql.test.tester.GraphQlTester.Response response,
                                    String expectedCode, String forbiddenMessageText, boolean expectNoPath) {
        response.errors().satisfy(errors -> {
            assertFalse(errors.isEmpty(), "the request should return a GraphQL error");
            var error = errors.get(0);
            assertEquals(expectedCode, error.getExtensions().get("code"));
            assertNotNull(error.getMessage());
            assertFalse(error.getMessage().isBlank());
            if (expectNoPath) {
                assertEquals("", error.getPath(), "request-level failures have no field execution path");
            } else {
                assertNotNull(error.getPath(), "resolver failures should identify the failing field path");
                assertFalse(error.getPath().isBlank(), "resolver failures should identify the failing field path");
            }
            if (forbiddenMessageText != null) {
                assertFalse(error.getMessage().contains(forbiddenMessageText));
            }
        });
    }

    private void executeMutation(String document, String variable, Object value) {
        graphQlTester.document(document).variable(variable, value).execute().errors().verify();
    }

    private Product saveProduct(String name, ProductStatus status, ProductUnit unit, String quantity) {
        return products.save(new Product(name, null, new BigDecimal(quantity), unit, BigDecimal.ONE, status));
    }
}
