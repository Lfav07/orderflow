package com.orderflow.inventory.infrastructure.graphql;

import com.orderflow.inventory.application.command.ChangeProductDescriptionCommand;
import com.orderflow.inventory.application.command.ChangeProductNameCommand;
import com.orderflow.inventory.application.command.ChangeProductPriceCommand;
import com.orderflow.inventory.application.command.ChangeProductUnitCommand;
import com.orderflow.inventory.application.command.ChangeStockCommand;
import com.orderflow.inventory.application.command.CreateProductCommand;
import com.orderflow.inventory.application.command.FilterProductsCommand;
import com.orderflow.inventory.application.command.GetProductByIdCommand;
import com.orderflow.inventory.application.command.ListProductsCommand;
import com.orderflow.inventory.application.command.ProductIdCommand;
import com.orderflow.inventory.application.usecase.ActivateProductUseCase;
import com.orderflow.inventory.application.usecase.ChangeProductDescriptionUseCase;
import com.orderflow.inventory.application.usecase.ChangeProductNameUseCase;
import com.orderflow.inventory.application.usecase.ChangeProductPriceUseCase;
import com.orderflow.inventory.application.usecase.ChangeProductUnitUseCase;
import com.orderflow.inventory.application.usecase.CreateProductUseCase;
import com.orderflow.inventory.application.usecase.DeactivateProductUseCase;
import com.orderflow.inventory.application.usecase.DecreaseStockUseCase;
import com.orderflow.inventory.application.usecase.FilterProductsUseCase;
import com.orderflow.inventory.application.usecase.GetAvailableStockUseCase;
import com.orderflow.inventory.application.usecase.GetProductByIdUseCase;
import com.orderflow.inventory.application.usecase.IncreaseStockUseCase;
import com.orderflow.inventory.application.usecase.ListProductsUseCase;
import com.orderflow.inventory.domain.model.Product;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.util.List;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Controller
public class ProductGraphQlController {

    private final GetProductByIdUseCase getProductByIdUseCase;
    private final ListProductsUseCase listProductsUseCase;
    private final FilterProductsUseCase filterProductsUseCase;
    private final GetAvailableStockUseCase getAvailableStockUseCase;
    private final CreateProductUseCase createProductUseCase;
    private final ChangeProductDescriptionUseCase changeProductDescriptionUseCase;
    private final ChangeProductNameUseCase changeProductNameUseCase;
    private final ChangeProductPriceUseCase changeProductPriceUseCase;
    private final ChangeProductUnitUseCase changeProductUnitUseCase;
    private final IncreaseStockUseCase increaseStockUseCase;
    private final DecreaseStockUseCase decreaseStockUseCase;
    private final ActivateProductUseCase activateProductUseCase;
    private final DeactivateProductUseCase deactivateProductUseCase;

    public ProductGraphQlController(
            GetProductByIdUseCase getProductByIdUseCase,
            ListProductsUseCase listProductsUseCase,
            FilterProductsUseCase filterProductsUseCase,
            GetAvailableStockUseCase getAvailableStockUseCase,
            CreateProductUseCase createProductUseCase,
            ChangeProductDescriptionUseCase changeProductDescriptionUseCase,
            ChangeProductNameUseCase changeProductNameUseCase,
            ChangeProductPriceUseCase changeProductPriceUseCase,
            ChangeProductUnitUseCase changeProductUnitUseCase,
            IncreaseStockUseCase increaseStockUseCase,
            DecreaseStockUseCase decreaseStockUseCase,
            ActivateProductUseCase activateProductUseCase,
            DeactivateProductUseCase deactivateProductUseCase) {
        this.getProductByIdUseCase = getProductByIdUseCase;
        this.listProductsUseCase = listProductsUseCase;
        this.filterProductsUseCase = filterProductsUseCase;
        this.getAvailableStockUseCase = getAvailableStockUseCase;
        this.createProductUseCase = createProductUseCase;
        this.changeProductDescriptionUseCase = changeProductDescriptionUseCase;
        this.changeProductNameUseCase = changeProductNameUseCase;
        this.changeProductPriceUseCase = changeProductPriceUseCase;
        this.changeProductUnitUseCase = changeProductUnitUseCase;
        this.increaseStockUseCase = increaseStockUseCase;
        this.decreaseStockUseCase = decreaseStockUseCase;
        this.activateProductUseCase = activateProductUseCase;
        this.deactivateProductUseCase = deactivateProductUseCase;
    }

    @QueryMapping
    public Product productById(@Argument("input") GetProductByIdCommand input) {
        return getProductByIdUseCase.execute(input);
    }

    @QueryMapping
    public List<Product> listProducts(@Argument("input") ListProductsCommand input) {
        return listProductsUseCase.execute(input);
    }

    @QueryMapping
    public List<Product> filterProducts(@Argument("input") FilterProductsCommand input) {
        return filterProductsUseCase.execute(input);
    }

    @QueryMapping
    public BigDecimal availableStock(@Argument("input") ProductIdCommand input) {
        return getAvailableStockUseCase.execute(input);
    }

    @MutationMapping
    public Product createProduct(@Argument("input") CreateProductCommand input) {
        return createProductUseCase.execute(input);
    }

    @MutationMapping
    public Product changeProductDescription(@Argument("input") ChangeProductDescriptionCommand input) {
        return changeProductDescriptionUseCase.execute(input);
    }

    @MutationMapping
    public Product changeProductName(@Argument("input") ChangeProductNameCommand input) {
        return changeProductNameUseCase.execute(input);
    }

    @MutationMapping
    public Product changeProductPrice(@Argument("input") ChangeProductPriceCommand input) {
        return changeProductPriceUseCase.execute(input);
    }

    @MutationMapping
    public Product changeProductUnit(@Argument("input") ChangeProductUnitCommand input) {
        return changeProductUnitUseCase.execute(input);
    }

    @MutationMapping
    public Product increaseStock(@Argument("input") ChangeStockCommand input) {
        return increaseStockUseCase.execute(input);
    }

    @MutationMapping
    public Product decreaseStock(@Argument("input") ChangeStockCommand input) {
        return decreaseStockUseCase.execute(input);
    }

    @MutationMapping
    public Product activateProduct(@Argument("input") ProductIdCommand input) {
        return activateProductUseCase.execute(input);
    }

    @MutationMapping
    public Product deactivateProduct(@Argument("input") ProductIdCommand input) {
        return deactivateProductUseCase.execute(input);
    }

    @SchemaMapping(typeName = "Product", field = "createdAt")
    public OffsetDateTime createdAt(Product product) {
        return product.getCreatedAt().atOffset(ZoneOffset.UTC);
    }

    @SchemaMapping(typeName = "Product", field = "updatedAt")
    public OffsetDateTime updatedAt(Product product) {
        return product.getUpdatedAt().atOffset(ZoneOffset.UTC);
    }
}
