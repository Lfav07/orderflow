package com.orderflow.inventory.infrastructure.graphql;

import com.orderflow.inventory.application.usecase.*;
import com.orderflow.inventory.domain.exception.StockOperationNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@GraphQlTest(ProductGraphQlController.class)
@Import({GraphQlConfig.class, InventoryGraphQlExceptionResolver.class})
class InventoryGraphQlErrorsTest {
    @Autowired GraphQlTester graphQl;
    @MockitoBean GetProductByIdUseCase getProductById;
    @MockitoBean ListProductsUseCase listProducts;
    @MockitoBean FilterProductsUseCase filterProducts;
    @MockitoBean GetAvailableStockUseCase getAvailableStock;
    @MockitoBean CreateProductUseCase createProduct;
    @MockitoBean ChangeProductDescriptionUseCase changeProductDescription;
    @MockitoBean ChangeProductNameUseCase changeProductName;
    @MockitoBean ChangeProductPriceUseCase changeProductPrice;
    @MockitoBean ChangeProductUnitUseCase changeProductUnit;
    @MockitoBean IncreaseStockUseCase increaseStock;
    @MockitoBean DecreaseStockUseCase decreaseStock;
    @MockitoBean ActivateProductUseCase activateProduct;
    @MockitoBean DeactivateProductUseCase deactivateProduct;
    @MockitoBean GetStockOperationByIdUseCase getStockOperationById;
    @MockitoBean ListStockOperationsUseCase listStockOperations;

    @Test
    void missingStockOperationHasNotFoundCode() {
        doThrow(new StockOperationNotFoundException(UUID.randomUUID())).when(getStockOperationById).execute(any());
        graphQl.document("query { stockOperationById(input:{operationId:\"00000000-0000-0000-0000-000000000000\"}) { operationId } }")
                .execute().errors().satisfy(errors -> {
                    assertEquals(1, errors.size());
                    assertEquals("NOT_FOUND", errors.getFirst().getExtensions().get("code"));
                    assertEquals("stockOperationById", errors.getFirst().getPath());
                    assertEquals("Stock operation was not found.", errors.getFirst().getMessage());
                });
    }

    @Test
    void invalidUuidHasBadRequestCode() {
        graphQl.document("query { productById(input:{id:\"invalid\"}) { id } }")
                .execute().errors().satisfy(errors -> {
                    assertEquals(1, errors.size());
                    assertEquals("BAD_REQUEST", errors.getFirst().getExtensions().get("code"));
                    assertEquals("productById", errors.getFirst().getPath());
                });
    }
}
