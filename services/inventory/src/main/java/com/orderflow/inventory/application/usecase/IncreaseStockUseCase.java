package com.orderflow.inventory.application.usecase;

import com.orderflow.inventory.application.command.ChangeStockCommand;
import com.orderflow.inventory.domain.model.Product;
import com.orderflow.inventory.domain.repository.ProductRepository;
import org.springframework.stereotype.Component;
import java.util.Objects;

@Component
public class IncreaseStockUseCase {
    private final ProductRepository productRepository;
    public IncreaseStockUseCase(ProductRepository productRepository) { this.productRepository = Objects.requireNonNull(productRepository); }
    public Product execute(ChangeStockCommand command) {
        Objects.requireNonNull(command, "Command cannot be null");
        Product product = productRepository.findById(command.id()); product.increaseStock(command.quantity()); return productRepository.save(product);
    }
}
