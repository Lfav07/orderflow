package com.orderflow.order.application.usecase;

import com.orderflow.order.application.command.TransitionOrderCommand;
import com.orderflow.order.application.gateway.InventoryGateway;
import com.orderflow.order.domain.model.Order;
import com.orderflow.order.domain.model.OrderItem;
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

/** Transitions an order to waiting for delivery and debits its items from inventory. */
@Component
public class MarkOrderWaitingDeliveryUseCase {
    private static final Logger log = LoggerFactory.getLogger(MarkOrderWaitingDeliveryUseCase.class);
    private final OrderRepository repository;
    private final InventoryGateway inventoryGateway;

    public MarkOrderWaitingDeliveryUseCase(OrderRepository repository, InventoryGateway inventoryGateway) {
        this.repository = Objects.requireNonNull(repository, "Order repository cannot be null");
        this.inventoryGateway = Objects.requireNonNull(inventoryGateway, "Inventory gateway cannot be null");
    }

    @Transactional
    public Order execute(TransitionOrderCommand command) {
        Objects.requireNonNull(command, "Command cannot be null");
        UUID id = Objects.requireNonNull(command.id(), "Order id cannot be null");
        Order order = findOrder(id);

        // Validate the transition before making any remote changes.
        order.markWaitingDelivery();
        List<StockOperationRequest> completedDebits = new ArrayList<>();
        boolean[] compensated = {false};
        registerRollbackCompensation(id, completedDebits, compensated);

        try {
            debitItems(order, completedDebits);
            return repository.save(order);
        } catch (RuntimeException failure) {
            compensated[0] = true;
            RuntimeException compensationFailure = compensate(completedDebits);
            if (compensationFailure != null) failure.addSuppressed(compensationFailure);
            throw failure;
        }
    }

    private Order findOrder(UUID id) {
        Order order = repository.findById(id);
        if (order == null) throw new NoSuchElementException("Order not found: " + id);
        return order;
    }

    /** Compensates successful debits if the transaction rolls back after execute returns. */
    private void registerRollbackCompensation(UUID id, List<StockOperationRequest> completedDebits,
                                              boolean[] compensated) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED && !compensated[0]) {
                        compensated[0] = true;
                        RuntimeException failure = compensate(completedDebits);
                        if (failure != null) log.error("Stock compensation failed for order {}", id, failure);
                    }
                }
            });
        }
    }

    private void debitItems(Order order, List<StockOperationRequest> completedDebits) {
        for (OrderItem item : order.getItems()) {
            StockOperationRequest request = new StockOperationRequest(UUID.randomUUID().toString(),
                    item.getProductId().toString(), item.getQuantity().toPlainString());
            inventoryGateway.debitStock(request);
            completedDebits.add(request);
        }
    }

    /** Returns confirmed debits in reverse order, collecting failures without stopping early. */
    private RuntimeException compensate(List<StockOperationRequest> completed) {
        RuntimeException failure = null;
        for (int i = completed.size() - 1; i >= 0; i--) {
            StockOperationRequest debit = completed.get(i);
            try {
                inventoryGateway.returnStock(new StockOperationRequest(UUID.randomUUID().toString(),
                        debit.productId(), debit.quantity()));
            } catch (RuntimeException ex) {
                if (failure == null) failure = ex;
                else failure.addSuppressed(ex);
            }
        }
        return failure;
    }
}
