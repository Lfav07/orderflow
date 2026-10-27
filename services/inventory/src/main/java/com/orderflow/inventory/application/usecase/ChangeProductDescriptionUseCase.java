package com.orderflow.inventory.application.usecase;

import com.orderflow.inventory.application.command.ChangeProductDescriptionCommand;
import com.orderflow.inventory.domain.model.Product;
import com.orderflow.inventory.domain.repository.ProductRepository;
import org.springframework.stereotype.Component;
import java.util.Objects;

@Component
public class ChangeProductDescriptionUseCase {
    private final ProductRepository productRepository;

    public ChangeProductDescriptionUseCase(ProductRepository productRepository) {
        this.productRepository = Objects.requireNonNull(productRepository, "Product repository cannot be null");
    }

    public Product execute(ChangeProductDescriptionCommand command) {
        Objects.requireNonNull(command, "Change product description command cannot be null");
        Product product = productRepository.findById(command.id());
        product.changeDescription(command.description());
        return productRepository.save(product);
    }
}
