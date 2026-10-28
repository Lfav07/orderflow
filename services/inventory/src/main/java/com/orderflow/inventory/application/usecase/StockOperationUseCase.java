package com.orderflow.inventory.application.usecase;

import com.orderflow.inventory.application.command.StockOperationCommand;
import com.orderflow.inventory.domain.model.Product;
import com.orderflow.inventory.domain.model.StockOperation;
import com.orderflow.inventory.domain.model.StockOperationType;
import com.orderflow.inventory.domain.repository.ProductRepository;
import com.orderflow.inventory.domain.repository.StockOperationRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

public abstract class StockOperationUseCase {
    private final ProductRepository productRepository;
    private final StockOperationRepository stockOperationRepository;
    private final StockOperationType expectedType;

    protected StockOperationUseCase(ProductRepository productRepository,
                          StockOperationRepository stockOperationRepository,
                          StockOperationType expectedType) {
        this.productRepository = Objects.requireNonNull(productRepository, "Product repository cannot be null");
        this.stockOperationRepository = Objects.requireNonNull(stockOperationRepository, "Stock operation repository cannot be null");
        this.expectedType = Objects.requireNonNull(expectedType, "Expected operation type cannot be null");
    }

    @Transactional
    public StockOperation execute(StockOperationCommand command) {
        Objects.requireNonNull(command, "Stock operation command cannot be null");
        if (command.type() != expectedType) {
            throw new IllegalArgumentException("Operation type must be " + expectedType);
        }

        Product product = productRepository.findById(command.productId());
        applyStockChange(product, command.quantity());
        productRepository.save(product);

        StockOperation operation = new StockOperation(
                command.operationId(), command.productId(), command.quantity(), command.type());
        return stockOperationRepository.save(operation);
    }

    protected abstract void applyStockChange(Product product, java.math.BigDecimal quantity);
}
