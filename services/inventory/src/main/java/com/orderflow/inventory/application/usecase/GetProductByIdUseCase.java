package com.orderflow.inventory.application.usecase;

import com.orderflow.inventory.application.command.GetProductByIdCommand;
import com.orderflow.inventory.domain.model.Product;
import com.orderflow.inventory.domain.repository.ProductRepository;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class GetProductByIdUseCase {
    private final ProductRepository productRepository;

    public GetProductByIdUseCase(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Product execute(GetProductByIdCommand command) {
        Objects.requireNonNull(command, "Get product command cannot be null");
        return productRepository.findById(command.id());
    }
}
