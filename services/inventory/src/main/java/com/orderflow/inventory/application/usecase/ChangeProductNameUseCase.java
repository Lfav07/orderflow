package com.orderflow.inventory.application.usecase;

import com.orderflow.inventory.application.command.ChangeProductNameCommand;
import com.orderflow.inventory.domain.model.Product;
import com.orderflow.inventory.domain.repository.ProductRepository;
import org.springframework.stereotype.Component;
import java.util.Objects;

@Component
public class ChangeProductNameUseCase {
    private final ProductRepository productRepository;

    public ChangeProductNameUseCase(ProductRepository productRepository) {
        this.productRepository = Objects.requireNonNull(productRepository, "Product repository cannot be null");
    }

    public Product execute(ChangeProductNameCommand command) {
        Objects.requireNonNull(command, "Change product name command cannot be null");
        Product product = productRepository.findById(command.id());
        product.changeName(command.name());
        return productRepository.save(product);
    }
}
