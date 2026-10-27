package com.orderflow.inventory.application.usecase;

import com.orderflow.inventory.application.command.ChangeProductUnitCommand;
import com.orderflow.inventory.domain.model.Product;
import com.orderflow.inventory.domain.repository.ProductRepository;
import org.springframework.stereotype.Component;
import java.util.Objects;

@Component
public class ChangeProductUnitUseCase {
    private final ProductRepository productRepository;
    public ChangeProductUnitUseCase(ProductRepository productRepository) { this.productRepository = Objects.requireNonNull(productRepository); }
    public Product execute(ChangeProductUnitCommand command) {
        Objects.requireNonNull(command, "Command cannot be null");
        Product product = productRepository.findById(command.id());
        product.changeUnit(command.unit());
        return productRepository.save(product);
    }
}
