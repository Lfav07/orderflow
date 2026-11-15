package com.orderflow.order.infrastructure.grpc;

import com.orderflow.order.grpc.*;
import com.orderflow.order.infrastructure.grpc.proto.ProductServiceGrpc;
import com.orderflow.order.infrastructure.grpc.proto.StockServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.grpc.client.ChannelBuilderOptions;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class InventoryGrpcClientTest {
    private Server server;
    private ManagedChannel channel;
    private InventoryGrpcClient client;
    private final AtomicReference<com.orderflow.order.infrastructure.grpc.proto.StockOperationRequest> stockRequest = new AtomicReference<>();
    private final AtomicReference<com.orderflow.order.infrastructure.grpc.proto.CheckStockAvailabilityRequest> availabilityRequest = new AtomicReference<>();
    private final AtomicReference<com.orderflow.order.infrastructure.grpc.proto.GetProductPriceRequest> priceRequest = new AtomicReference<>();

    @BeforeEach
    void setUp() throws IOException {
        String name = InProcessServerBuilder.generateName();
        server = InProcessServerBuilder.forName(name).directExecutor()
                .addService(new StockServiceGrpc.StockServiceImplBase() {
                    @Override
                    public void debitStock(com.orderflow.order.infrastructure.grpc.proto.StockOperationRequest request,
                            StreamObserver<com.orderflow.order.infrastructure.grpc.proto.StockOperationResponse> observer) {
                        stockRequest.set(request);
                        if (request.getProductId().equals("missing")) {
                            observer.onError(Status.NOT_FOUND.withDescription("Product was not found.").asRuntimeException());
                            return;
                        }
                        respond(request, com.orderflow.order.infrastructure.grpc.proto.StockOperationType.DEBIT, observer);
                    }
                    @Override
                    public void returnStock(com.orderflow.order.infrastructure.grpc.proto.StockOperationRequest request,
                            StreamObserver<com.orderflow.order.infrastructure.grpc.proto.StockOperationResponse> observer) {
                        stockRequest.set(request);
                        respond(request, com.orderflow.order.infrastructure.grpc.proto.StockOperationType.RETURN, observer);
                    }
                    @Override
                    public void checkStockAvailability(com.orderflow.order.infrastructure.grpc.proto.CheckStockAvailabilityRequest request,
                            StreamObserver<com.orderflow.order.infrastructure.grpc.proto.CheckStockAvailabilityResponse> observer) {
                        availabilityRequest.set(request);
                        observer.onNext(com.orderflow.order.infrastructure.grpc.proto.CheckStockAvailabilityResponse.newBuilder()
                                .setProductId(request.getProductId()).setAvailable(true).build());
                        observer.onCompleted();
                    }
                }).addService(new ProductServiceGrpc.ProductServiceImplBase() {
                    @Override
                    public void getProductPrice(com.orderflow.order.infrastructure.grpc.proto.GetProductPriceRequest request,
                            StreamObserver<com.orderflow.order.infrastructure.grpc.proto.GetProductPriceResponse> observer) {
                        priceRequest.set(request);
                        observer.onNext(com.orderflow.order.infrastructure.grpc.proto.GetProductPriceResponse.newBuilder()
                                .setProductId(request.getProductId()).setUnitPrice("12.50").build());
                        observer.onCompleted();
                    }
                }).build().start();
        channel = InProcessChannelBuilder.forName(name).directExecutor().build();
        GrpcChannelFactory factory = new GrpcChannelFactory() {
            @Override
            public boolean supports(String target) { return true; }

            @Override
            public ManagedChannel createChannel(String target, ChannelBuilderOptions options) {
                assertEquals("inventory", target);
                return channel;
            }
        };
        client = new InventoryGrpcClient(factory);
    }

    private static void respond(com.orderflow.order.infrastructure.grpc.proto.StockOperationRequest request,
            com.orderflow.order.infrastructure.grpc.proto.StockOperationType type,
            StreamObserver<com.orderflow.order.infrastructure.grpc.proto.StockOperationResponse> observer) {
        observer.onNext(com.orderflow.order.infrastructure.grpc.proto.StockOperationResponse.newBuilder()
                .setOperationId(request.getOperationId()).setProductId(request.getProductId())
                .setQuantity(request.getQuantity()).setType(type).setCreatedAt("2026-01-01T00:00:00Z").build());
        observer.onCompleted();
    }

    @AfterEach
    void tearDown() {
        channel.shutdownNow();
        server.shutdownNow();
    }

    @Test
    void stockOperationsSendAndReturnEveryField() {
        var request = new StockOperationRequest("operation", "product", "3.50");
        assertEquals(new StockOperationResponse("operation", "product", "3.50", StockOperationType.DEBIT,
                "2026-01-01T00:00:00Z"), client.debitStock(request));
        assertEquals("operation", stockRequest.get().getOperationId());
        assertEquals("product", stockRequest.get().getProductId());
        assertEquals("3.50", stockRequest.get().getQuantity());
        assertEquals(new StockOperationResponse("operation", "product", "3.50", StockOperationType.RETURN,
                "2026-01-01T00:00:00Z"), client.returnStock(request));
    }

    @Test
    void availabilityAndPriceUseTheInventoryResponses() {
        assertEquals(new CheckStockAvailabilityResponse("product", true),
                client.checkStockAvailability(new CheckStockAvailabilityRequest("product", "2.500")));
        assertEquals("product", availabilityRequest.get().getProductId());
        assertEquals("2.500", availabilityRequest.get().getQuantity());
        assertEquals(new GetProductPriceResponse("product", "12.50"),
                client.getProductPrice(new GetProductPriceRequest("product")));
        assertEquals("product", priceRequest.get().getProductId());
    }

    @Test
    void serverStatusIsPropagatedToTheCaller() {
        var error = assertThrows(StatusRuntimeException.class,
                () -> client.debitStock(new StockOperationRequest("operation", "missing", "1")));
        assertEquals(Status.Code.NOT_FOUND, error.getStatus().getCode());
        assertEquals("Product was not found.", error.getStatus().getDescription());
    }
}
