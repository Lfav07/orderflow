package com.orderflow.inventory.application.usecase;

import com.orderflow.inventory.application.command.CreateProductCommand;
import com.orderflow.inventory.domain.model.Product;
import com.orderflow.inventory.domain.repository.ProductRepository;

import java.util.Objects;

public class CreateProductUseCase {
    private final ProductRepository productRepository;

    public CreateProductUseCase(ProductRepository productRepository) {
        this.productRepository = Objects.requireNonNull(productRepository, "Product repository cannot be null");
    }

    public Product execute(CreateProductCommand command) {
        Objects.requireNonNull(command, "Create product command cannot be null");
        Product product = new Product(
                command.name(),
                command.description(),
                command.quantityInStock(),
                command.unit(),
                command.pricePerUnit(),
                command.status()
        );
        return productRepository.save(product);
    }
}
