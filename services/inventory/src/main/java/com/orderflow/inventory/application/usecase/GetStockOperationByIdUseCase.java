package com.orderflow.inventory.application.usecase;

import com.orderflow.inventory.application.command.GetStockOperationByIdCommand;
import com.orderflow.inventory.domain.model.StockOperation;
import com.orderflow.inventory.domain.repository.StockOperationRepository;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class GetStockOperationByIdUseCase {
    private final StockOperationRepository stockOperationRepository;

    public GetStockOperationByIdUseCase(StockOperationRepository stockOperationRepository) {
        this.stockOperationRepository = Objects.requireNonNull(stockOperationRepository);
    }

    public StockOperation execute(GetStockOperationByIdCommand command) {
        Objects.requireNonNull(command, "Get stock operation command cannot be null");
        return stockOperationRepository.findById(command.operationId());
    }
}
