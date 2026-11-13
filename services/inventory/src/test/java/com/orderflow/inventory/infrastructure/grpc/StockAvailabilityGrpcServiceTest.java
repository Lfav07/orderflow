package com.orderflow.inventory.infrastructure.grpc;

import com.orderflow.inventory.application.usecase.CheckStockAvailabilityUseCase;
import com.orderflow.inventory.domain.model.Product;
import com.orderflow.inventory.domain.model.ProductStatus;
import com.orderflow.inventory.domain.model.ProductUnit;
import com.orderflow.inventory.domain.repository.ProductRepository;
import com.orderflow.inventory.grpc.CheckStockAvailabilityRequest;
import com.orderflow.inventory.grpc.CheckStockAvailabilityResponse;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class StockAvailabilityGrpcServiceTest {
    @Test
    void returnsAvailabilityForRequestedQuantity() {
        Product product = new Product("Coffee", null, new BigDecimal("4.5"), ProductUnit.UNITS,
                BigDecimal.ONE, ProductStatus.ACTIVE);
        ProductRepository repository = (ProductRepository) Proxy.newProxyInstance(
                ProductRepository.class.getClassLoader(), new Class<?>[]{ProductRepository.class},
                (proxy, method, args) -> {
                    assertEquals("findById", method.getName());
                    assertEquals(product.getId(), args[0]);
                    return product;
                });
        StockGrpcService service = new StockGrpcService(null, null, new CheckStockAvailabilityUseCase(repository));
        for (String quantity : new String[]{"4.500", "4.501"}) {
            CapturingObserver observer = new CapturingObserver();
            service.checkStockAvailability(CheckStockAvailabilityRequest.newBuilder()
                    .setProductId(product.getId().toString()).setQuantity(quantity).build(), observer);
            assertEquals(product.getId().toString(), observer.response.getProductId());
            assertEquals(quantity.equals("4.500"), observer.response.getAvailable());
            assertTrue(observer.completed);
        }
        assertEquals(new BigDecimal("4.5"), product.getQuantityInStock());
    }

    private static class CapturingObserver implements StreamObserver<CheckStockAvailabilityResponse> {
        CheckStockAvailabilityResponse response;
        boolean completed;
        public void onNext(CheckStockAvailabilityResponse value) { response = value; }
        public void onError(Throwable error) { throw new AssertionError(error); }
        public void onCompleted() { completed = true; }
    }
}
