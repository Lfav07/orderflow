package com.orderflow.order.grpc;

public record StockOperationResponse(
        String operationId, String productId, String quantity, StockOperationType type, String createdAt) {}
