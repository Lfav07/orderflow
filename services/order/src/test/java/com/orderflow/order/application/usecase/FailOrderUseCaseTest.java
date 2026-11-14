package com.orderflow.order.application.usecase;

import com.orderflow.order.application.command.TransitionOrderCommand;
import com.orderflow.order.domain.model.OrderStatus;
import com.orderflow.order.grpc.StockOperationRequest;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FailOrderUseCaseTest {
    private final MarkOrderWaitingDeliveryUseCaseTest.Repo repo = new MarkOrderWaitingDeliveryUseCaseTest.Repo();
    private final MarkOrderWaitingDeliveryUseCaseTest.Inventory stock = new MarkOrderWaitingDeliveryUseCaseTest.Inventory();
    private final FailOrderUseCase useCase = new FailOrderUseCase(repo, stock);

    @Test
    void returnsEveryItemFromEitherDeliveryState() {
        for (OrderStatus state : new OrderStatus[]{OrderStatus.WAITING_DELIVERY, OrderStatus.IN_DELIVERY}) {
            repo.order = MarkOrderWaitingDeliveryUseCaseTest.order(state);
            stock.returns.clear();
            useCase.execute(new TransitionOrderCommand(repo.order.getId()));
            assertEquals(OrderStatus.FAILED, repo.order.getStatus());
            assertEquals(repo.order.getItems().size(), stock.returns.size());
            for (int i = 0; i < stock.returns.size(); i++) {
                StockOperationRequest request = stock.returns.get(i);
                assertEquals(repo.order.getItems().get(i).getProductId().toString(), request.productId());
                assertEquals(repo.order.getItems().get(i).getQuantity().toPlainString(), request.quantity());
                UUID.fromString(request.operationId());
            }
            assertNotEquals(stock.returns.get(0).operationId(), stock.returns.get(1).operationId());
        }
    }

    @Test
    void waitingPaymentAndInvalidTransitionsDoNotReturnStock() {
        repo.order = MarkOrderWaitingDeliveryUseCaseTest.order(OrderStatus.WAITING_PAYMENT);
        useCase.execute(new TransitionOrderCommand(repo.order.getId()));
        assertTrue(stock.returns.isEmpty());
        assertThrows(IllegalStateException.class, () -> useCase.execute(new TransitionOrderCommand(repo.order.getId())));
        assertTrue(stock.returns.isEmpty());
    }

    @Test
    void saveFailureReversesReturnsInReverseOrder() {
        repo.order = MarkOrderWaitingDeliveryUseCaseTest.order(OrderStatus.IN_DELIVERY);
        repo.failSave = true;
        assertThrows(IllegalStateException.class, () -> useCase.execute(new TransitionOrderCommand(repo.order.getId())));
        assertEquals(2, stock.debits.size());
        assertEquals(stock.returns.get(1).productId(), stock.debits.get(0).productId());
        assertEquals(stock.returns.get(0).productId(), stock.debits.get(1).productId());
        assertEquals(stock.returns.get(1).quantity(), stock.debits.get(0).quantity());
        assertNotEquals(stock.returns.get(1).operationId(), stock.debits.get(0).operationId());
    }

    @Test
    void rollbackAfterReturnReversesReturnsOnce() {
        repo.order = MarkOrderWaitingDeliveryUseCaseTest.order(OrderStatus.WAITING_DELIVERY);
        TransactionSynchronizationManager.initSynchronization();
        try {
            useCase.execute(new TransitionOrderCommand(repo.order.getId()));
            TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
            assertEquals(2, stock.debits.size());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void returnFailureDoesNotSaveOrder() {
        repo.order = MarkOrderWaitingDeliveryUseCaseTest.order(OrderStatus.IN_DELIVERY);
        stock.failFirstReturn = true;
        assertThrows(IllegalStateException.class, () -> useCase.execute(new TransitionOrderCommand(repo.order.getId())));
        assertEquals(0, repo.saves);
        assertTrue(stock.debits.isEmpty());
    }
}
