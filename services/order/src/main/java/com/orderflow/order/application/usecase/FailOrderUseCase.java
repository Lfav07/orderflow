package com.orderflow.order.application.usecase;

import com.orderflow.order.application.command.TransitionOrderCommand;
import com.orderflow.order.application.gateway.InventoryGateway;
import com.orderflow.order.domain.model.Order;
import com.orderflow.order.domain.model.OrderItem;
import com.orderflow.order.domain.model.OrderStatus;
import com.orderflow.order.domain.repository.OrderRepository;
import com.orderflow.order.grpc.StockOperationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;

@Component
public class FailOrderUseCase {
    private static final Logger log = LoggerFactory.getLogger(FailOrderUseCase.class);
    private final OrderRepository repository;
    private final InventoryGateway inventoryGateway;

    public FailOrderUseCase(OrderRepository repository, InventoryGateway inventoryGateway) {
        this.repository = Objects.requireNonNull(repository, "Order repository cannot be null");
        this.inventoryGateway = Objects.requireNonNull(inventoryGateway, "Inventory gateway cannot be null");
    }

    @Transactional
    public Order execute(TransitionOrderCommand command) {
        Objects.requireNonNull(command, "Command cannot be null");
        UUID id = Objects.requireNonNull(command.id(), "Order id cannot be null");
        Order order = repository.findById(id);
        if (order == null) throw new NoSuchElementException("Order not found: " + id);

        boolean returnItems = order.getStatus() == OrderStatus.WAITING_DELIVERY
                || order.getStatus() == OrderStatus.IN_DELIVERY;
        order.fail(); // Validate before changing inventory.
        if (!returnItems) return repository.save(order);

        List<StockOperationRequest> completedReturns = new ArrayList<>();
        boolean[] compensated = {false};
        registerRollbackCompensation(id, completedReturns, compensated);
        try {
            for (OrderItem item : order.getItems()) {
                StockOperationRequest request = new StockOperationRequest(UUID.randomUUID().toString(),
                        item.getProductId().toString(), item.getQuantity().toPlainString());
                inventoryGateway.returnStock(request);
                completedReturns.add(request);
            }
            return repository.save(order);
        } catch (RuntimeException failure) {
            compensated[0] = true;
            RuntimeException compensationFailure = compensate(completedReturns);
            if (compensationFailure != null) failure.addSuppressed(compensationFailure);
            throw failure;
        }
    }

    private void registerRollbackCompensation(UUID id, List<StockOperationRequest> completedReturns,
                                              boolean[] compensated) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED && !compensated[0]) {
                        compensated[0] = true;
                        RuntimeException failure = compensate(completedReturns);
                        if (failure != null) log.error("Stock compensation failed for order {}", id, failure);
                    }
                }
            });
        }
    }

    /** Reverses confirmed returns in reverse order, collecting any debit failures. */
    private RuntimeException compensate(List<StockOperationRequest> completedReturns) {
        RuntimeException failure = null;
        for (int i = completedReturns.size() - 1; i >= 0; i--) {
            StockOperationRequest returned = completedReturns.get(i);
            try {
                inventoryGateway.debitStock(new StockOperationRequest(UUID.randomUUID().toString(),
                        returned.productId(), returned.quantity()));
            } catch (RuntimeException ex) {
                if (failure == null) failure = ex;
                else failure.addSuppressed(ex);
            }
        }
        return failure;
    }
}
