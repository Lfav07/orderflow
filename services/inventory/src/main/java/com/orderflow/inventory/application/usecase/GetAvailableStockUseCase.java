package com.orderflow.inventory.application.usecase;

import com.orderflow.inventory.application.command.ProductIdCommand;
import com.orderflow.inventory.domain.repository.ProductRepository;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.Objects;

@Component
public class GetAvailableStockUseCase {
    private final ProductRepository productRepository;
    public GetAvailableStockUseCase(ProductRepository productRepository) { this.productRepository = Objects.requireNonNull(productRepository); }
    public BigDecimal execute(ProductIdCommand command) {
        Objects.requireNonNull(command, "Command cannot be null");
        return productRepository.findById(command.id()).getQuantityInStock();
    }
}
