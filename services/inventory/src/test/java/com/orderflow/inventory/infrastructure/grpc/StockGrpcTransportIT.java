package com.orderflow.inventory.infrastructure.grpc;

import com.orderflow.inventory.application.usecase.DebitStockUseCase;
import com.orderflow.inventory.application.usecase.ReturnStockUseCase;
import com.orderflow.inventory.domain.model.*;
import com.orderflow.inventory.domain.repository.ProductRepository;
import com.orderflow.inventory.domain.repository.StockOperationRepository;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/** Exercises the generated blocking stub, gRPC transport, service, use cases, and domain behavior in process. */
class StockGrpcTransportIT {
    @Test
    void debitAndReturnRpcCallsUpdateStockAndPersistOperationHistory() throws Exception {
        var state = new InventoryState();
        Product product = state.addProduct("10");
        String serverName = InProcessServerBuilder.generateName();
        Server server = InProcessServerBuilder.forName(serverName).directExecutor()
                .addService(new StockGrpcService(new DebitStockUseCase(state.products, state.operations),
                        new ReturnStockUseCase(state.products, state.operations)))
                .build().start();
        ManagedChannel channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();
        try {
            var stub = com.orderflow.inventory.grpc.StockServiceGrpc.newBlockingStub(channel);
            UUID debitId = UUID.randomUUID();
            var debit = stub.debitStock(request(debitId, product.getId(), "3.5"));

            assertEquals(debitId.toString(), debit.getOperationId());
            assertEquals("3.5", debit.getQuantity());
            assertEquals(com.orderflow.inventory.grpc.StockOperationType.DEBIT, debit.getType());
            assertEquals(new BigDecimal("6.5"), product.getQuantityInStock());

            UUID returnId = UUID.randomUUID();
            var returned = stub.returnStock(request(returnId, product.getId(), "1.25"));
            assertEquals(returnId.toString(), returned.getOperationId());
            assertEquals(com.orderflow.inventory.grpc.StockOperationType.RETURN, returned.getType());
            assertEquals(new BigDecimal("7.75"), product.getQuantityInStock());
            assertEquals(2, state.savedOperations.size());
        } finally {
            channel.shutdownNow();
            server.shutdownNow();
        }
    }

    private static com.orderflow.inventory.grpc.StockOperationRequest request(UUID operationId, UUID productId, String quantity) {
        return com.orderflow.inventory.grpc.StockOperationRequest.newBuilder().setOperationId(operationId.toString())
                .setProductId(productId.toString()).setQuantity(quantity).build();
    }

    private static final class InventoryState {
        private final Map<UUID, Product> productRows = new HashMap<>();
        private final List<StockOperation> savedOperations = new ArrayList<>();
        private final ProductRepository products = new ProductRepository() {
            public Product save(Product product) { productRows.put(product.getId(), product); return product; }
            public Product findById(UUID id) { return Objects.requireNonNull(productRows.get(id)); }
            public List<Product> findAll(int page, int size) { return List.copyOf(productRows.values()); }
            public List<Product> findByFilters(ProductStatus status, String name, ProductUnit unit, int page, int size) { return List.of(); }
        };
        private final StockOperationRepository operations = new StockOperationRepository() {
            public StockOperation save(StockOperation operation) { savedOperations.add(operation); return operation; }
            public StockOperation findById(UUID id) { return savedOperations.stream().filter(op -> op.getOperationId().equals(id)).findFirst().orElse(null); }
            public List<StockOperation> findAllByFilters(UUID productId, StockOperationType type, int page, int size) { return List.copyOf(savedOperations); }
        };
        private Product addProduct(String quantity) {
            Product product = new Product("RPC item", null, new BigDecimal(quantity), ProductUnit.UNITS, BigDecimal.ONE, ProductStatus.ACTIVE);
            productRows.put(product.getId(), product);
            return product;
        }
    }
}
