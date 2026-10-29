package com.orderflow.inventory.application.usecase;

import com.orderflow.inventory.application.command.CreateProductCommand;
import com.orderflow.inventory.domain.exception.NegativePriceException;
import com.orderflow.inventory.domain.model.Product;
import com.orderflow.inventory.domain.model.ProductStatus;
import com.orderflow.inventory.domain.model.ProductUnit;
import com.orderflow.inventory.domain.repository.ProductRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class CreateProductUseCaseTest {
    @Test
    void createsAndSavesProductFromCommand() {
        AtomicReference<Product> savedProduct = new AtomicReference<>();
        ProductRepository repository = repositoryFor(product -> {
            savedProduct.set(product);
            return product;
        });
        CreateProductUseCase useCase = new CreateProductUseCase(repository);
        CreateProductCommand command = new CreateProductCommand(" Café ", " Grãos ",
                new BigDecimal("2.5"), ProductUnit.KILOGRAMS, new BigDecimal("12.00"), ProductStatus.ACTIVE);

        Product result = useCase.execute(command);

        assertEquals("Café", result.getName());
        assertEquals("Grãos", result.getDescription());
        assertEquals(new BigDecimal("2.5"), result.getQuantityInStock());
        assertEquals(ProductUnit.KILOGRAMS, result.getUnit());
        assertEquals(new BigDecimal("12.00"), result.getPricePerUnit());
        assertEquals(ProductStatus.ACTIVE, result.getStatus());
        assertSame(result, savedProduct.get());
    }

    @Test
    void propagatesDomainValidationFailureWithoutSaving() {
        AtomicReference<Product> savedProduct = new AtomicReference<>();
        ProductRepository repository = repositoryFor(product -> {
            savedProduct.set(product);
            return product;
        });
        CreateProductUseCase useCase = new CreateProductUseCase(repository);
        CreateProductCommand command = new CreateProductCommand("Café", null,
                BigDecimal.ZERO, ProductUnit.UNITS, new BigDecimal("-1"), ProductStatus.ACTIVE);

        assertThrows(NegativePriceException.class, () -> useCase.execute(command));
        assertNull(savedProduct.get());
    }

    @Test
    void rejectsNullCommandAndRepository() {
        assertThrows(NullPointerException.class, () -> new CreateProductUseCase(null));

        CreateProductUseCase useCase = new CreateProductUseCase(repositoryFor(product -> product));
        assertThrows(NullPointerException.class, () -> useCase.execute(null));
    }

    private static ProductRepository repositoryFor(java.util.function.Function<Product, Product> save) {
        return new ProductRepository() {
            @Override
            public Product save(Product product) {
                return save.apply(product);
            }

            @Override
            public Product findById(UUID id) {
                return null;
            }

            @Override
            public List<Product> findAll(int page, int size) {
                return List.of();
            }

            @Override
            public List<Product> findByFilters(ProductStatus status, String name, ProductUnit unit, int page, int size) {
                return List.of();
            }
        };
    }
}
