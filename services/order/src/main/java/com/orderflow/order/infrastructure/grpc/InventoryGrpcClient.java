package com.orderflow.order.infrastructure.grpc;

import com.orderflow.order.application.gateway.InventoryGateway;
import com.orderflow.order.grpc.*;
import com.orderflow.order.infrastructure.grpc.proto.ProductServiceGrpc;
import com.orderflow.order.infrastructure.grpc.proto.StockServiceGrpc;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.stereotype.Component;

@Component
public class InventoryGrpcClient implements InventoryGateway {
    private final StockServiceGrpc.StockServiceBlockingStub stock;
    private final ProductServiceGrpc.ProductServiceBlockingStub product;

    public InventoryGrpcClient(GrpcChannelFactory channels) {
        var channel = channels.createChannel("inventory");
        this.stock = StockServiceGrpc.newBlockingStub(channel);
        this.product = ProductServiceGrpc.newBlockingStub(channel);
    }

    @Override
    public StockOperationResponse debitStock(StockOperationRequest request) {
        return toStockResponse(stock.debitStock(toStockRequest(request)));
    }

    @Override
    public StockOperationResponse returnStock(StockOperationRequest request) {
        return toStockResponse(stock.returnStock(toStockRequest(request)));
    }

    @Override
    public CheckStockAvailabilityResponse checkStockAvailability(CheckStockAvailabilityRequest request) {
        var response = stock.checkStockAvailability(
                com.orderflow.order.infrastructure.grpc.proto.CheckStockAvailabilityRequest.newBuilder()
                        .setProductId(request.productId()).setQuantity(request.quantity()).build());
        return new CheckStockAvailabilityResponse(response.getProductId(), response.getAvailable());
    }

    @Override
    public GetProductPriceResponse getProductPrice(GetProductPriceRequest request) {
        var response = product.getProductPrice(
                com.orderflow.order.infrastructure.grpc.proto.GetProductPriceRequest.newBuilder()
                        .setProductId(request.productId()).build());
        return new GetProductPriceResponse(response.getProductId(), response.getUnitPrice());
    }

    private static com.orderflow.order.infrastructure.grpc.proto.StockOperationRequest toStockRequest(
            StockOperationRequest request) {
        return com.orderflow.order.infrastructure.grpc.proto.StockOperationRequest.newBuilder()
                .setOperationId(request.operationId()).setProductId(request.productId())
                .setQuantity(request.quantity()).build();
    }

    private static StockOperationResponse toStockResponse(
            com.orderflow.order.infrastructure.grpc.proto.StockOperationResponse response) {
        return new StockOperationResponse(response.getOperationId(), response.getProductId(), response.getQuantity(),
                StockOperationType.valueOf(response.getType().name()), response.getCreatedAt());
    }
}
