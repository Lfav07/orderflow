package com.orderflow.inventory.application.usecase;

import com.orderflow.inventory.application.command.ListProductsCommand;
import com.orderflow.inventory.domain.model.Product;
import com.orderflow.inventory.domain.repository.ProductRepository;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Objects;

@Component
public class ListProductsUseCase {
    private final ProductRepository productRepository;

    public ListProductsUseCase(ProductRepository productRepository) {
        this.productRepository = Objects.requireNonNull(productRepository);
    }

    public List<Product> execute(ListProductsCommand command) {
        Objects.requireNonNull(command, "List products command cannot be null");
        validatePage(command.page(), command.size());
        return productRepository.findAll(command.page(), command.size());
    }

    static void validatePage(int page, int size) {
        if (page < 0 || size < 1) throw new IllegalArgumentException("Page must be non-negative and size must be positive");
    }
}
