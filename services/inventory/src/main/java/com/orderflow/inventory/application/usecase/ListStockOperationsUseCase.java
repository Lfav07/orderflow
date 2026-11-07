package com.orderflow.inventory.application.usecase;

import com.orderflow.inventory.application.command.ListStockOperationsCommand;
import com.orderflow.inventory.domain.model.StockOperation;
import com.orderflow.inventory.domain.repository.StockOperationRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class ListStockOperationsUseCase {
    private final StockOperationRepository stockOperationRepository;

    public ListStockOperationsUseCase(StockOperationRepository stockOperationRepository) {
        this.stockOperationRepository = Objects.requireNonNull(stockOperationRepository);
    }

    public List<StockOperation> execute(ListStockOperationsCommand command) {
        Objects.requireNonNull(command, "List stock operations command cannot be null");
        ListProductsUseCase.validatePage(command.page(), command.size());
        return stockOperationRepository.findAllByFilters(
                command.productId(), command.type(), command.page(), command.size());
    }
}
