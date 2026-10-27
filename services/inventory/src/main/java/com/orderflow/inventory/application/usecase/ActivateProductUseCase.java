package com.orderflow.inventory.application.usecase;

import com.orderflow.inventory.application.command.ProductIdCommand;
import com.orderflow.inventory.domain.model.Product;
import com.orderflow.inventory.domain.repository.ProductRepository;
import org.springframework.stereotype.Component;
import java.util.Objects;

@Component
public class ActivateProductUseCase {
    private final ProductRepository productRepository;
    public ActivateProductUseCase(ProductRepository productRepository) { this.productRepository = Objects.requireNonNull(productRepository); }
    public Product execute(ProductIdCommand command) {
        Objects.requireNonNull(command, "Command cannot be null");
        Product product = productRepository.findById(command.id()); product.activate(); return productRepository.save(product);
    }
}
