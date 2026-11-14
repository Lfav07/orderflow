package com.orderflow.order.application.usecase;

import com.orderflow.order.application.command.TransitionOrderCommand;
import com.orderflow.order.application.gateway.InventoryGateway;
import com.orderflow.order.domain.model.*;
import com.orderflow.order.domain.repository.OrderRepository;
import com.orderflow.order.grpc.*;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class MarkOrderWaitingDeliveryUseCaseTest {
    final Repo repo = new Repo();
    final Inventory stock = new Inventory();
    final MarkOrderWaitingDeliveryUseCase useCase = new MarkOrderWaitingDeliveryUseCase(repo, stock);

    @Test void successfulTransition() {
        repo.order = order(OrderStatus.WAITING_PAYMENT);
        assertSame(repo.order, useCase.execute(new TransitionOrderCommand(repo.order.getId())));
        assertEquals(OrderStatus.WAITING_DELIVERY, repo.order.getStatus());
        assertEquals(2, stock.debits.size());
        assertTrue(stock.returns.isEmpty());
    }
    @Test void saveFailureCompensatesInReverseOrderAndContinuesAfterReturnFailure() {
        repo.order = order(OrderStatus.WAITING_PAYMENT);
        repo.failSave = true; stock.failFirstReturn = true;
        RuntimeException ex = assertThrows(RuntimeException.class, () -> useCase.execute(new TransitionOrderCommand(repo.order.getId())));
        assertEquals("save", ex.getMessage());
        assertEquals("return", ex.getSuppressed()[0].getMessage());
        assertEquals(2, stock.returns.size());
        for (int i = 0; i < 2; i++) {
            var debit = stock.debits.get(1-i); var returned = stock.returns.get(i);
            assertEquals(debit.productId(), returned.productId());
            assertEquals(debit.quantity(), returned.quantity());
            assertNotEquals(debit.operationId(), returned.operationId());
            UUID.fromString(returned.operationId());
        }
    }
    @Test void partialDebitFailureCompensatesConfirmedDebit() {
        repo.order = order(OrderStatus.WAITING_PAYMENT); stock.failSecondDebit = true;
        assertThrows(IllegalStateException.class, () -> useCase.execute(new TransitionOrderCommand(repo.order.getId())));
        assertEquals(1, stock.returns.size());
        assertEquals(0, repo.saves);
    }
    @Test void rollbackAfterReturnCompensates() {
        repo.order = order(OrderStatus.WAITING_PAYMENT);
        TransactionSynchronizationManager.initSynchronization();
        try {
            useCase.execute(new TransitionOrderCommand(repo.order.getId()));
            TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
            assertEquals(2, stock.returns.size());
        } finally { TransactionSynchronizationManager.clearSynchronization(); }
    }
    @Test void invalidInputDoesNotDebit() {
        repo.order = order(OrderStatus.DELIVERED);
        assertThrows(IllegalStateException.class, () -> useCase.execute(new TransitionOrderCommand(repo.order.getId())));
        assertThrows(NoSuchElementException.class, () -> useCase.execute(new TransitionOrderCommand(UUID.randomUUID())));
        assertThrows(NullPointerException.class, () -> useCase.execute(null));
        assertThrows(NullPointerException.class, () -> useCase.execute(new TransitionOrderCommand(null)));
        assertTrue(stock.debits.isEmpty());
    }
    static Order order(OrderStatus status) {
        UUID id = UUID.randomUUID(); Instant now = Instant.now();
        return Order.rehydrate(id, status, List.of(new OrderItem(id, UUID.randomUUID(), BigDecimal.ONE, BigDecimal.ONE), new OrderItem(id, UUID.randomUUID(), new BigDecimal("2"), BigDecimal.ONE)), 1L, now, now);
    }
    static class Repo implements OrderRepository {
        Order order; boolean failSave; int saves;
        public Order findById(UUID id) { return order != null && order.getId().equals(id) ? order : null; }
        public Order save(Order o) { saves++; if (failSave) throw new IllegalStateException("save"); return o; }
        public List<Order> findAll(int p, int s) { throw new UnsupportedOperationException(); }
        public List<Order> findByFilters(OrderStatus status, int p, int s) { throw new UnsupportedOperationException(); }
    }
    static class Inventory implements InventoryGateway {
        final List<StockOperationRequest> debits = new ArrayList<>(), returns = new ArrayList<>();
        boolean failSecondDebit, failFirstReturn;
        public StockOperationResponse debitStock(StockOperationRequest r) { if (failSecondDebit && debits.size() == 1) throw new IllegalStateException("debit"); debits.add(r); return null; }
        public StockOperationResponse returnStock(StockOperationRequest r) { returns.add(r); if (failFirstReturn && returns.size() == 1) throw new IllegalStateException("return"); return null; }
        public CheckStockAvailabilityResponse checkStockAvailability(CheckStockAvailabilityRequest r) { throw new UnsupportedOperationException(); }
        public GetProductPriceResponse getProductPrice(GetProductPriceRequest r) { throw new UnsupportedOperationException(); }
    }
}
