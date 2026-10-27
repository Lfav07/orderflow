package com.orderflow.inventory.application.usecase;

import com.orderflow.inventory.application.command.ChangeProductPriceCommand;
import com.orderflow.inventory.domain.model.Product;
import com.orderflow.inventory.domain.repository.ProductRepository;
import org.springframework.stereotype.Component;
import java.util.Objects;

@Component
public class ChangeProductPriceUseCase {
    private final ProductRepository productRepository;
    public ChangeProductPriceUseCase(ProductRepository productRepository) { this.productRepository = Objects.requireNonNull(productRepository); }
    public Product execute(ChangeProductPriceCommand command) {
        Objects.requireNonNull(command, "Command cannot be null");
        Product product = productRepository.findById(command.id());
        product.changePricePerUnit(command.pricePerUnit());
        return productRepository.save(product);
    }
}
