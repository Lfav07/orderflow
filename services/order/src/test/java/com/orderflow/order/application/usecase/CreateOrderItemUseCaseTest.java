package com.orderflow.order.application.usecase;

import com.orderflow.order.application.command.CreateOrderItemCommand;
import com.orderflow.order.application.gateway.InsufficientStockException;
import com.orderflow.order.application.gateway.InventoryGateway;
import com.orderflow.order.grpc.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CreateOrderItemUseCaseTest {
    private final FakeInventory inventory = new FakeInventory();
    private final CreateOrderItemUseCase useCase = new CreateOrderItemUseCase(inventory);
    private final UUID orderId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();

    @Test
    void createsItemWithInventoryPriceSnapshot() {
        var quantity = new BigDecimal("2.5");
        var item = useCase.execute(new CreateOrderItemCommand(orderId, productId, quantity));
        assertEquals(new CheckStockAvailabilityRequest(productId.toString(), "2.5"), inventory.stockRequest);
        assertEquals(new GetProductPriceRequest(productId.toString()), inventory.priceRequest);
        assertEquals(orderId, item.getOrderId());
        assertEquals(productId, item.getProductId());
        assertEquals(quantity, item.getQuantity());
        assertEquals(new BigDecimal("3.20"), item.getUnitPrice());
        assertEquals(0, new BigDecimal("8.00").compareTo(item.getTotalAmount()));
    }

    @Test
    void unavailableStockStopsBeforePriceLookup() {
        inventory.available = false;
        assertThrows(InsufficientStockException.class,
                () -> useCase.execute(new CreateOrderItemCommand(orderId, productId, BigDecimal.ONE)));
        assertNull(inventory.priceRequest);
    }

    @Test
    void invalidInputDoesNotCallInventory() {
        assertThrows(NullPointerException.class, () -> useCase.execute(null));
        assertThrows(NullPointerException.class, () -> useCase.execute(new CreateOrderItemCommand(null, productId, BigDecimal.ONE)));
        assertThrows(NullPointerException.class, () -> useCase.execute(new CreateOrderItemCommand(orderId, null, BigDecimal.ONE)));
        assertThrows(NullPointerException.class, () -> useCase.execute(new CreateOrderItemCommand(orderId, productId, null)));
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(new CreateOrderItemCommand(orderId, productId, BigDecimal.ZERO)));
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(new CreateOrderItemCommand(orderId, productId, BigDecimal.ONE.negate())));
        assertNull(inventory.stockRequest);
        assertNull(inventory.priceRequest);
    }

    private static class FakeInventory implements InventoryGateway {
        boolean available = true;
        CheckStockAvailabilityRequest stockRequest;
        GetProductPriceRequest priceRequest;
        public CheckStockAvailabilityResponse checkStockAvailability(CheckStockAvailabilityRequest request) {
            stockRequest = request;
            return new CheckStockAvailabilityResponse(request.productId(), available);
        }
        public GetProductPriceResponse getProductPrice(GetProductPriceRequest request) {
            priceRequest = request;
            return new GetProductPriceResponse(request.productId(), "3.20");
        }
        public StockOperationResponse debitStock(StockOperationRequest request) { throw new AssertionError("Unexpected debit"); }
        public StockOperationResponse returnStock(StockOperationRequest request) { throw new AssertionError("Unexpected return"); }
    }
}
