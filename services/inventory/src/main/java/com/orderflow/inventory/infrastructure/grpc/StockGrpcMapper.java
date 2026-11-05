package com.orderflow.inventory.infrastructure.grpc;

import com.orderflow.inventory.application.command.StockOperationCommand;
import com.orderflow.inventory.domain.model.StockOperation;


import java.math.BigDecimal;
import java.util.UUID;

public  class StockGrpcMapper {
    static StockOperationCommand toStockOperationCommand(com.orderflow.inventory.grpc.StockOperationRequest request){
        return new StockOperationCommand(
                UUID.fromString(request.getOperationId()),
                UUID.fromString(request.getProductId()),
                new BigDecimal(request.getQuantity())
        );
    }
    static com.orderflow.inventory.grpc.StockOperationResponse toStockOperationResponse(StockOperation operation){
        return com.orderflow.inventory.grpc.StockOperationResponse.newBuilder()
                .setOperationId(operation.getOperationId().toString())
                .setProductId(operation.getProductId().toString())
                .setQuantity(operation.getQuantity().toString())
                .setType(com.orderflow.inventory.grpc.StockOperationType.valueOf(operation.getType().toString()))
                .setCreatedAt(operation.getCreatedAt().toString()).build();
    }
}
