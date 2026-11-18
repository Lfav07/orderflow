package com.orderflow.inventory.application.usecase;

import com.orderflow.inventory.application.command.ChangeStockCommand;
import com.orderflow.inventory.domain.model.Product;
import com.orderflow.inventory.domain.model.ProductStatus;
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
        Product product = productRepository.findById(command.id());
        boolean enoughStock = product.hasEnoughStock(command.quantity());
        return product.getStatus() == ProductStatus.ACTIVE && enoughStock;
    }
}
