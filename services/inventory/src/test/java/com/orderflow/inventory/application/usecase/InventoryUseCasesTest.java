package com.orderflow.inventory.application.usecase;

import com.orderflow.inventory.application.command.*;
import com.orderflow.inventory.domain.exception.InsufficientStockException;
import com.orderflow.inventory.domain.model.*;
import com.orderflow.inventory.domain.repository.ProductRepository;
import com.orderflow.inventory.domain.repository.StockOperationRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class InventoryUseCasesTest {
    @Test
    void readUseCasesDelegateToRepositoriesAndReturnResults() {
        Fixture f = new Fixture();
        Product product = f.product("Coffee", "4");
        StockOperation op = new StockOperation(UUID.randomUUID(), product.getId(), BigDecimal.ONE, StockOperationType.RETURN);
        f.operationRows.put(op.getOperationId(), op);

        assertSame(product, new GetProductByIdUseCase(f.products).execute(new GetProductByIdCommand(product.getId())));
        assertEquals(new BigDecimal("4"), new GetAvailableStockUseCase(f.products).execute(new ProductIdCommand(product.getId())));
        assertSame(op, new GetStockOperationByIdUseCase(f.operationRepository).execute(new GetStockOperationByIdCommand(op.getOperationId())));
        assertEquals(List.of(product), new ListProductsUseCase(f.products).execute(new ListProductsCommand(0, 5)));
        assertEquals(List.of(product), new FilterProductsUseCase(f.products).execute(new FilterProductsCommand(null, " Coffee ", null, 0, 5)));
        assertEquals(List.of(op), new ListStockOperationsUseCase(f.operationRepository).execute(new ListStockOperationsCommand(null, null, 0, 5)));
        assertEquals("Coffee", f.lastFilterName);
    }

    @Test
    void listingRejectsInvalidPaginationBeforeRepositoryCall() {
        Fixture f = new Fixture();
        assertThrows(IllegalArgumentException.class, () -> new ListProductsUseCase(f.products).execute(new ListProductsCommand(-1, 2)));
        assertThrows(IllegalArgumentException.class, () -> new ListProductsUseCase(f.products).execute(new ListProductsCommand(0, 0)));
        assertThrows(IllegalArgumentException.class, () -> new ListStockOperationsUseCase(f.operationRepository).execute(new ListStockOperationsCommand(null, null, 0, 0)));
        assertEquals(0, f.listCalls);
    }

    @Test
    void productMutationUseCasesUpdateAndSaveTheFoundProduct() {
        Fixture f = new Fixture();
        Product p = f.product("Coffee", "5");
        UUID id = p.getId();
        new ChangeProductNameUseCase(f.products).execute(new ChangeProductNameCommand(id, " Tea "));
        new ChangeProductDescriptionUseCase(f.products).execute(new ChangeProductDescriptionCommand(id, " Leaf "));
        new ChangeProductPriceUseCase(f.products).execute(new ChangeProductPriceCommand(id, new BigDecimal("7.25")));
        new ChangeProductUnitUseCase(f.products).execute(new ChangeProductUnitCommand(id, ProductUnit.GRAMS));
        new DecreaseStockUseCase(f.products).execute(new ChangeStockCommand(id, BigDecimal.ONE));
        new IncreaseStockUseCase(f.products).execute(new ChangeStockCommand(id, new BigDecimal("2")));
        new DeactivateProductUseCase(f.products).execute(new ProductIdCommand(id));
        new ActivateProductUseCase(f.products).execute(new ProductIdCommand(id));

        assertEquals("Tea", p.getName());
        assertEquals("Leaf", p.getDescription());
        assertEquals(new BigDecimal("7.25"), p.getPricePerUnit());
        assertEquals(ProductUnit.GRAMS, p.getUnit());
        assertEquals(new BigDecimal("6"), p.getQuantityInStock());
        assertEquals(ProductStatus.ACTIVE, p.getStatus());
        assertEquals(8, f.productSaves);
    }

    @Test
    void failedProductMutationDoesNotSave() {
        Fixture f = new Fixture();
        Product p = f.product("Coffee", "1");
        assertThrows(InsufficientStockException.class,
                () -> new DecreaseStockUseCase(f.products).execute(new ChangeStockCommand(p.getId(), new BigDecimal("2"))));
        assertEquals(0, f.productSaves);
        assertEquals(BigDecimal.ONE, p.getQuantityInStock());
    }

    @Test
    void stockOperationUseCasesApplyExpectedStockChangeAndPersistOperation() {
        for (StockOperationType type : StockOperationType.values()) {
            Fixture f = new Fixture();
            Product p = f.product("Item", "10");
            UUID operationId = UUID.randomUUID();
            StockOperationCommand command = new StockOperationCommand(operationId, p.getId(), new BigDecimal("2"));
            StockOperation result = switch (type) {
                case DEBIT -> new DebitStockUseCase(f.products, f.operationRepository).execute(command);
                case RETURN -> new ReturnStockUseCase(f.products, f.operationRepository).execute(command);
                case ADJUSTMENT_IN -> new AdjustmentInStockUseCase(f.products, f.operationRepository).execute(command);
                case ADJUSTMENT_OUT -> new AdjustmentOutStockUseCase(f.products, f.operationRepository).execute(command);
            };
            BigDecimal expectedStock = (type == StockOperationType.DEBIT || type == StockOperationType.ADJUSTMENT_OUT)
                    ? new BigDecimal("8") : new BigDecimal("12");
            assertEquals(expectedStock, p.getQuantityInStock(), type + " must apply the correct stock delta");
            assertEquals(type, result.getType());
            assertSame(result, f.savedOperation);
            assertEquals(1, f.productSaves);
        }
    }

    @Test
    void rejectedStockOperationDoesNotPersistProductOrHistory() {
        Fixture f = new Fixture();
        Product p = f.product("Item", "1");
        StockOperationCommand command = new StockOperationCommand(UUID.randomUUID(), p.getId(), new BigDecimal("2"));
        assertThrows(InsufficientStockException.class, () -> new DebitStockUseCase(f.products, f.operationRepository).execute(command));
        assertEquals(0, f.productSaves);
        assertNull(f.savedOperation);
    }

    private static final class Fixture {
        final Map<UUID, Product> productRows = new LinkedHashMap<>();
        final Map<UUID, StockOperation> operationRows = new LinkedHashMap<>();
        int productSaves;
        int listCalls;
        String lastFilterName;
        final ProductRepository products = new ProductRepository() {
            public Product save(Product p) { productSaves++; productRows.put(p.getId(), p); return p; }
            public Product findById(UUID id) { return Objects.requireNonNull(productRows.get(id)); }
            public List<Product> findAll(int page, int size) { listCalls++; return List.copyOf(productRows.values()); }
            public List<Product> findByFilters(ProductStatus status, String name, ProductUnit unit, int page, int size) {
                listCalls++; lastFilterName = name;
                return productRows.values().stream().filter(p -> status == null || p.getStatus() == status)
                        .filter(p -> name == null || p.getName().contains(name)).filter(p -> unit == null || p.getUnit() == unit).toList();
            }
        };
        StockOperation savedOperation;
        final StockOperationRepository operationRepository = new StockOperationRepository() {
            public StockOperation save(StockOperation op) { savedOperation = op; operationRows.put(op.getOperationId(), op); return op; }
            public StockOperation findById(UUID id) { return operationRows.get(id); }
            public List<StockOperation> findAllByFilters(UUID productId, StockOperationType type, int page, int size) {
                listCalls++;
                return operationRows.values().stream().filter(o -> productId == null || o.getProductId().equals(productId))
                        .filter(o -> type == null || o.getType() == type).toList();
            }
        };
        Product product(String name, String quantity) {
            Product p = new Product(name, null, new BigDecimal(quantity), ProductUnit.UNITS, BigDecimal.ONE, ProductStatus.ACTIVE);
            productRows.put(p.getId(), p); return p;
        }
    }
}
