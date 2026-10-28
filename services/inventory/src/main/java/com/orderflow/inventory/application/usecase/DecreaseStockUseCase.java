package com.orderflow.inventory.application.usecase;

import com.orderflow.inventory.application.command.ChangeStockCommand;
import com.orderflow.inventory.domain.model.Product;
import com.orderflow.inventory.domain.repository.ProductRepository;
import org.springframework.stereotype.Component;
import java.util.Objects;

@Component
public class DecreaseStockUseCase {
    private final ProductRepository productRepository;
    public DecreaseStockUseCase(ProductRepository productRepository) { this.productRepository = Objects.requireNonNull(productRepository); }
    public Product execute(ChangeStockCommand command) {
        Objects.requireNonNull(command, "Command cannot be null");
        Product product = productRepository.findById(command.id()); product.decreaseStock(command.quantity()); return productRepository.save(product);
    }
}
