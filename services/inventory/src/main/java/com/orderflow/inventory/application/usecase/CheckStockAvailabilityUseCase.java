package com.orderflow.inventory.application.usecase;

import com.orderflow.inventory.application.command.ChangeStockCommand;
import com.orderflow.inventory.domain.repository.ProductRepository;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class CheckStockAvailabilityUseCase {
    private final ProductRepository productRepository;

    public CheckStockAvailabilityUseCase(ProductRepository productRepository) {
        this.productRepository = Objects.requireNonNull(productRepository);
    }

    public boolean execute(ChangeStockCommand command) {
        Objects.requireNonNull(command, "Command cannot be null");
        return productRepository.findById(command.id()).hasEnoughStock(command.quantity());
    }
}
