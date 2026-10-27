package com.orderflow.inventory.application.usecase;

import com.orderflow.inventory.application.command.FilterProductsCommand;
import com.orderflow.inventory.domain.model.Product;
import com.orderflow.inventory.domain.repository.ProductRepository;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Objects;

@Component
public class FilterProductsUseCase {
    private final ProductRepository productRepository;

    public FilterProductsUseCase(ProductRepository productRepository) {
        this.productRepository = Objects.requireNonNull(productRepository);
    }

    public List<Product> execute(FilterProductsCommand command) {
        Objects.requireNonNull(command, "Filter products command cannot be null");
        ListProductsUseCase.validatePage(command.page(), command.size());
        String name = command.name() == null || command.name().isBlank() ? null : command.name().strip();
        return productRepository.findByFilters(command.status(), name, command.unit(), command.page(), command.size());
    }
}
