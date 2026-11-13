package com.orderflow.inventory.infrastructure.grpc;

import com.orderflow.inventory.application.usecase.GetProductPriceUseCase;
import com.orderflow.inventory.domain.model.Product;
import com.orderflow.inventory.domain.model.ProductStatus;
import com.orderflow.inventory.domain.model.ProductUnit;
import com.orderflow.inventory.domain.repository.ProductRepository;
import com.orderflow.inventory.grpc.GetProductPriceRequest;
import com.orderflow.inventory.grpc.GetProductPriceResponse;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ProductGrpcServiceTest {
    @Test
    void returnsPriceAndCompletesResponse() {
        Product product = new Product("Coffee", null, BigDecimal.ONE, ProductUnit.UNITS,
                new BigDecimal("12.50"), ProductStatus.ACTIVE);
        ProductRepository repository = (ProductRepository) Proxy.newProxyInstance(
                ProductRepository.class.getClassLoader(), new Class<?>[]{ProductRepository.class},
                (proxy, method, args) -> {
                    assertEquals("findById", method.getName());
                    assertEquals(product.getId(), args[0]);
                    return product;
                });
        CapturingObserver observer = new CapturingObserver();

        new ProductGrpcService(new GetProductPriceUseCase(repository)).getProductPrice(
                GetProductPriceRequest.newBuilder().setProductId(product.getId().toString()).build(), observer);

        assertEquals(product.getId().toString(), observer.response.getProductId());
        assertEquals("12.50", observer.response.getUnitPrice());
        assertTrue(observer.completed);
        assertNull(observer.error);
    }

    @Test
    void rejectsMalformedIdBeforeCallingUseCase() {
        ProductRepository repository = (ProductRepository) Proxy.newProxyInstance(
                ProductRepository.class.getClassLoader(), new Class<?>[]{ProductRepository.class},
                (proxy, method, args) -> { throw new AssertionError("Repository must not be called"); });
        CapturingObserver observer = new CapturingObserver();

        assertThrows(IllegalArgumentException.class, () -> new ProductGrpcService(new GetProductPriceUseCase(repository))
                .getProductPrice(GetProductPriceRequest.newBuilder().setProductId("bad-id").build(), observer));
        assertNull(observer.response);
        assertFalse(observer.completed);
    }

    private static class CapturingObserver implements StreamObserver<GetProductPriceResponse> {
        GetProductPriceResponse response;
        Throwable error;
        boolean completed;
        public void onNext(GetProductPriceResponse value) { response = value; }
        public void onError(Throwable throwable) { error = throwable; }
        public void onCompleted() { completed = true; }
    }
}
