package com.orderflow.inventory.infrastructure.grpc;

import com.orderflow.inventory.domain.exception.StockOperationNotFoundException;
import com.orderflow.inventory.domain.model.Product;
import com.orderflow.inventory.domain.model.ProductStatus;
import com.orderflow.inventory.domain.model.ProductUnit;
import com.orderflow.inventory.domain.repository.ProductRepository;
import com.orderflow.inventory.domain.repository.StockOperationRepository;
import com.orderflow.inventory.infrastructure.persistence.ProductRepositoryAdapter;
import com.orderflow.inventory.testsupport.PostgresTestSupport;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Exercises the Spring-managed gRPC advice through an in-process client and PostgreSQL-backed repositories. */
@SpringBootTest(properties = {
        "spring.grpc.test.inprocess.enabled=true",
        "spring.grpc.server.inprocess.name=inventory-error-tests"
})
@Import(StockGrpcTransportIT.FailingProductRepositoryConfiguration.class)
class StockGrpcTransportIT extends PostgresTestSupport {
    private static final String SENTINEL = "SENSITIVE_SENTINEL_DATABASE_DETAIL";
    private static final UUID TRIGGER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Autowired ProductRepository products;
    @Autowired StockOperationRepository operations;

    private ManagedChannel channel;
    private com.orderflow.inventory.grpc.StockServiceGrpc.StockServiceBlockingStub stub;
    private com.orderflow.inventory.grpc.ProductServiceGrpc.ProductServiceBlockingStub productStub;

    @BeforeEach
    void connectToInProcessServer() {
        channel = InProcessChannelBuilder.forName("inventory-error-tests").directExecutor().build();
        stub = com.orderflow.inventory.grpc.StockServiceGrpc.newBlockingStub(channel);
        productStub = com.orderflow.inventory.grpc.ProductServiceGrpc.newBlockingStub(channel);
    }

    @AfterEach
    void closeChannel() {
        if (channel != null) channel.shutdownNow();
    }

    @Test
    void stockAvailabilityChecksExactAndInsufficientQuantitiesWithoutChangingStock() {
        Product product = saveProduct("4.5");
        String id = product.getId().toString();

        var exact = stub.checkStockAvailability(availabilityRequest(id, "4.500"));
        assertEquals(id, exact.getProductId());
        assertTrue(exact.getAvailable());
        assertFalse(stub.checkStockAvailability(availabilityRequest(id, "4.501")).getAvailable());
        assertEquals(0, products.findById(product.getId()).getQuantityInStock().compareTo(new BigDecimal("4.5")));
    }

    @Test
    void stockAvailabilityMapsInvalidRequestsAndMissingProducts() {
        Product product = saveProduct("4.5");
        String id = product.getId().toString();
        for (String quantity : List.of("", "bad", "0", "-1", "0.0001")) {
            var error = assertThrows(StatusRuntimeException.class,
                    () -> stub.checkStockAvailability(availabilityRequest(id, quantity)));
            assertEquals(Status.Code.INVALID_ARGUMENT, error.getStatus().getCode(), quantity);
        }
        var invalidId = assertThrows(StatusRuntimeException.class,
                () -> stub.checkStockAvailability(availabilityRequest("bad-id", "1")));
        assertEquals(Status.Code.INVALID_ARGUMENT, invalidId.getStatus().getCode());
        var missing = assertThrows(StatusRuntimeException.class,
                () -> stub.checkStockAvailability(availabilityRequest(UUID.randomUUID().toString(), "1")));
        assertEquals(Status.Code.NOT_FOUND, missing.getStatus().getCode());
    }

    private static com.orderflow.inventory.grpc.CheckStockAvailabilityRequest availabilityRequest(String id, String quantity) {
        return com.orderflow.inventory.grpc.CheckStockAvailabilityRequest.newBuilder()
                .setProductId(id).setQuantity(quantity).build();
    }

    @Test
    void productPriceRpcReturnsPersistedDecimalPrice() {
        Product product = saveProduct("10");
        product.changePricePerUnit(new BigDecimal("12.50"));
        products.save(product);

        var response = productStub.getProductPrice(priceRequest(product.getId().toString()));

        assertEquals(product.getId().toString(), response.getProductId());
        assertEquals(0, new BigDecimal("12.50").compareTo(new BigDecimal(response.getUnitPrice())));
    }

    @Test
    void productPriceRpcMapsUnknownAndMalformedIds() {
        StatusRuntimeException missing = assertThrows(StatusRuntimeException.class,
                () -> productStub.getProductPrice(priceRequest(UUID.randomUUID().toString())));
        assertEquals(Status.Code.NOT_FOUND, missing.getStatus().getCode());
        assertSafeDescription(missing, "ProductNotFoundException");

        for (String id : List.of("bad-id", "")) {
            StatusRuntimeException invalid = assertThrows(StatusRuntimeException.class,
                    () -> productStub.getProductPrice(priceRequest(id)));
            assertEquals(Status.Code.INVALID_ARGUMENT, invalid.getStatus().getCode());
            assertSafeDescription(invalid, "IllegalArgumentException");
        }
    }

    @Test
    void productPriceRpcHidesUnexpectedFailures() {
        StatusRuntimeException error = assertThrows(StatusRuntimeException.class,
                () -> productStub.getProductPrice(priceRequest(TRIGGER_ID.toString())));
        assertEquals(Status.Code.INTERNAL, error.getStatus().getCode());
        assertSafeDescription(error, SENTINEL);
    }

    private static com.orderflow.inventory.grpc.GetProductPriceRequest priceRequest(String id) {
        return com.orderflow.inventory.grpc.GetProductPriceRequest.newBuilder().setProductId(id).build();
    }

    @Test
    void debitAndReturnRpcCallsUpdateStockAndPersistOperationHistory() {
        Product product = saveProduct("10");
        UUID debitId = UUID.randomUUID();
        var debit = stub.debitStock(request(debitId, product.getId(), "3.5"));

        assertEquals(debitId.toString(), debit.getOperationId());
        assertEquals("3.5", debit.getQuantity());
        assertEquals(com.orderflow.inventory.grpc.StockOperationType.DEBIT, debit.getType());
        assertEquals(0, products.findById(product.getId()).getQuantityInStock().compareTo(new BigDecimal("6.5")));
        assertEquals(debitId, operations.findById(debitId).getOperationId());

        UUID returnId = UUID.randomUUID();
        var returned = stub.returnStock(request(returnId, product.getId(), "1.25"));
        assertEquals(returnId.toString(), returned.getOperationId());
        assertEquals(com.orderflow.inventory.grpc.StockOperationType.RETURN, returned.getType());
        assertEquals(0, products.findById(product.getId()).getQuantityInStock().compareTo(new BigDecimal("7.75")));
        assertEquals(returnId, operations.findById(returnId).getOperationId());
    }

    @Test
    void insufficientStockIsFailedPreconditionWithSafeDescriptionAndNoOperationSaved() {
        Product product = saveProduct("1");
        UUID operationId = UUID.randomUUID();

        StatusRuntimeException error = assertThrows(StatusRuntimeException.class,
                () -> stub.debitStock(request(operationId, product.getId(), "2")));

        assertEquals(Status.Code.FAILED_PRECONDITION, error.getStatus().getCode());
        assertSafeDescription(error, "InsufficientStockException");
        assertEquals(0, products.findById(product.getId()).getQuantityInStock().compareTo(BigDecimal.ONE));
        assertOperationDoesNotExist(operationId);
    }

    @Test
    void unknownProductIsNotFoundWithSafeDescriptionAndNoOperationSaved() {
        UUID operationId = UUID.randomUUID();
        StatusRuntimeException error = assertThrows(StatusRuntimeException.class,
                () -> stub.returnStock(request(operationId, UUID.randomUUID(), "1")));

        assertEquals(Status.Code.NOT_FOUND, error.getStatus().getCode());
        assertSafeDescription(error, "ProductNotFoundException");
        assertOperationDoesNotExist(operationId);
    }

    @Test
    void malformedUuidIsInvalidArgumentWithoutSideEffects() {
        Product product = saveProduct("10");
        UUID operationId = UUID.randomUUID();
        var request = request(operationId, product.getId(), "1").toBuilder().setProductId("bad-id").build();

        StatusRuntimeException error = assertThrows(StatusRuntimeException.class, () -> stub.debitStock(request));

        assertEquals(Status.Code.INVALID_ARGUMENT, error.getStatus().getCode());
        assertSafeDescription(error, "IllegalArgumentException");
        assertEquals(0, products.findById(product.getId()).getQuantityInStock().compareTo(new BigDecimal("10")));
        assertOperationDoesNotExist(operationId);
    }

    @Test
    void malformedDecimalIsInvalidArgumentWithoutSideEffects() {
        Product product = saveProduct("10");
        UUID operationId = UUID.randomUUID();

        StatusRuntimeException error = assertThrows(StatusRuntimeException.class,
                () -> stub.debitStock(request(operationId, product.getId(), "not-a-decimal")));

        assertEquals(Status.Code.INVALID_ARGUMENT, error.getStatus().getCode());
        assertSafeDescription(error, "NumberFormatException");
        assertEquals(0, products.findById(product.getId()).getQuantityInStock().compareTo(new BigDecimal("10")));
        assertOperationDoesNotExist(operationId);
    }

    @Test
    void zeroQuantityIsInvalidArgumentWithoutSideEffects() {
        Product product = saveProduct("10");
        UUID operationId = UUID.randomUUID();

        StatusRuntimeException error = assertThrows(StatusRuntimeException.class,
                () -> stub.debitStock(request(operationId, product.getId(), "0")));

        assertEquals(Status.Code.INVALID_ARGUMENT, error.getStatus().getCode());
        assertEquals(0, products.findById(product.getId()).getQuantityInStock().compareTo(new BigDecimal("10")));
        assertOperationDoesNotExist(operationId);
    }

    @Test
    void quantityWithUnsupportedScaleIsInvalidArgumentWithoutSideEffects() {
        Product product = saveProduct("10");
        UUID operationId = UUID.randomUUID();

        StatusRuntimeException error = assertThrows(StatusRuntimeException.class,
                () -> stub.debitStock(request(operationId, product.getId(), "1.0001")));

        assertEquals(Status.Code.INVALID_ARGUMENT, error.getStatus().getCode());
        assertEquals(0, products.findById(product.getId()).getQuantityInStock().compareTo(new BigDecimal("10")));
        assertOperationDoesNotExist(operationId);
    }

    @Test
    void unexpectedRepositoryFailureIsInternalAndDoesNotExposeExceptionDetails() {
        UUID operationId = UUID.randomUUID();
        StatusRuntimeException error = assertThrows(StatusRuntimeException.class,
                () -> stub.debitStock(request(operationId, TRIGGER_ID, "1")));

        assertEquals(Status.Code.INTERNAL, error.getStatus().getCode());
        assertSafeDescription(error, SENTINEL);
        assertFalse(error.getStatus().getDescription().contains("IllegalStateException"));
        assertOperationDoesNotExist(operationId);
    }

    private Product saveProduct(String quantity) {
        return products.save(new Product("RPC-" + UUID.randomUUID().toString().substring(0, 12), null, new BigDecimal(quantity),
                ProductUnit.UNITS, BigDecimal.ONE, ProductStatus.ACTIVE));
    }

    private void assertOperationDoesNotExist(UUID operationId) {
        assertThrows(StockOperationNotFoundException.class, () -> operations.findById(operationId));
    }

    private static com.orderflow.inventory.grpc.StockOperationRequest request(UUID operationId, UUID productId,
                                                                               String quantity) {
        return com.orderflow.inventory.grpc.StockOperationRequest.newBuilder()
                .setOperationId(operationId.toString())
                .setProductId(productId.toString())
                .setQuantity(quantity)
                .build();
    }

    private static void assertSafeDescription(StatusRuntimeException error, String forbiddenText) {
        String description = error.getStatus().getDescription();
        assertNotNull(description);
        assertFalse(description.isBlank());
        assertFalse(description.contains(forbiddenText));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FailingProductRepositoryConfiguration {
        @Bean
        @Primary
        ProductRepository productRepositoryWithSentinelFailure(
                @Qualifier("productRepositoryAdapter") ProductRepositoryAdapter delegate) {
            return new ProductRepository() {
                @Override
                public Product save(Product product) {
                    return delegate.save(product);
                }

                @Override
                public Product findById(UUID id) {
                    if (TRIGGER_ID.equals(id)) throw new IllegalStateException(SENTINEL);
                    return delegate.findById(id);
                }

                @Override
                public List<Product> findAll(int page, int size) {
                    return delegate.findAll(page, size);
                }

                @Override
                public List<Product> findByFilters(ProductStatus status, String name, ProductUnit unit,
                                                   int page, int size) {
                    return delegate.findByFilters(status, name, unit, page, size);
                }
            };
        }
    }
}
