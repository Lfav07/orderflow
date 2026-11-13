package com.orderflow.order.application.usecase;

import com.orderflow.order.application.command.CreateOrderCommand;
import com.orderflow.order.application.command.OrderItemCommand;
import com.orderflow.order.application.gateway.InsufficientStockException;
import com.orderflow.order.application.gateway.InventoryGateway;
import com.orderflow.order.domain.model.Order;
import com.orderflow.order.domain.model.OrderItem;
import com.orderflow.order.domain.model.OrderStatus;
import com.orderflow.order.domain.repository.OrderItemRepository;
import com.orderflow.order.domain.repository.OrderRepository;
import com.orderflow.order.grpc.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CreateOrderUseCaseTest {
    private final Fixture fixture = new Fixture();
    private final OrderItemRepository items = new OrderItemRepository() {
        public OrderItem save(OrderItem item) { throw new AssertionError("Expected batch save"); }
        public List<OrderItem> saveAll(List<OrderItem> items) { fixture.savedItems = List.copyOf(items); return fixture.savedItems; }
        public OrderItem findById(UUID id) { throw new UnsupportedOperationException(); }
        public List<OrderItem> findAllByOrderId(UUID id) { throw new UnsupportedOperationException(); }
    };
    private final CreateOrderUseCase useCase = new CreateOrderUseCase(fixture, items, new CreateOrderItemUseCase(fixture));
    private final UUID productId = UUID.randomUUID();

    @Test
    void createsOrderAndSavesItsItemsTogether() {
        Order order = useCase.execute(new CreateOrderCommand(List.of(
                new OrderItemCommand(productId, new BigDecimal("2")),
                new OrderItemCommand(UUID.randomUUID(), BigDecimal.ONE))));

        assertSame(fixture.savedOrder, order);
        assertEquals(OrderStatus.WAITING_PAYMENT, order.getStatus());
        assertEquals(order.getItems(), fixture.savedItems);
        assertEquals(2, fixture.savedItems.size());
        assertTrue(fixture.savedItems.stream().allMatch(item -> item.getOrderId().equals(order.getId())));
        assertEquals(0, new BigDecimal("9.60").compareTo(order.getTotalAmount()));
    }

    @Test
    void invalidLaterItemIsRejectedBeforeAnyInventoryCallsOrWrites() {
        var command = new CreateOrderCommand(List.of(
                new OrderItemCommand(productId, BigDecimal.ONE),
                new OrderItemCommand(UUID.randomUUID(), BigDecimal.ZERO)));
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(command));
        assertThrows(NullPointerException.class, () -> useCase.execute(null));
        assertThrows(NullPointerException.class, () -> useCase.execute(new CreateOrderCommand(null)));
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(new CreateOrderCommand(List.of())));
        assertEquals(0, fixture.inventoryCalls);
        assertNull(fixture.savedOrder);
        assertNull(fixture.savedItems);
    }

    @Test
    void itemCreationFailureLeavesNothingSaved() {
        fixture.available = false;
        assertThrows(InsufficientStockException.class, () -> useCase.execute(
                new CreateOrderCommand(List.of(new OrderItemCommand(productId, BigDecimal.ONE)))));
        assertNull(fixture.savedOrder);
        assertNull(fixture.savedItems);
    }

    private static class Fixture implements OrderRepository, InventoryGateway {
        Order savedOrder;
        List<OrderItem> savedItems;
        int inventoryCalls;
        boolean available = true;

        public Order save(Order order) { savedOrder = order; return order; }
        public Order findById(UUID id) { throw new UnsupportedOperationException(); }
        public List<Order> findAll(int page, int size) { throw new UnsupportedOperationException(); }
        public List<Order> findByFilters(OrderStatus status, int page, int size) { throw new UnsupportedOperationException(); }
        public CheckStockAvailabilityResponse checkStockAvailability(CheckStockAvailabilityRequest request) {
            inventoryCalls++;
            return new CheckStockAvailabilityResponse(request.productId(), available);
        }
        public GetProductPriceResponse getProductPrice(GetProductPriceRequest request) {
            inventoryCalls++;
            return new GetProductPriceResponse(request.productId(), "3.20");
        }
        public StockOperationResponse debitStock(StockOperationRequest request) { throw new AssertionError("Unexpected debit"); }
        public StockOperationResponse returnStock(StockOperationRequest request) { throw new AssertionError("Unexpected return"); }
    }
}
