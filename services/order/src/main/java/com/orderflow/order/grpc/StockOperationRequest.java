package com.orderflow.order.grpc;

public record StockOperationRequest(String operationId, String productId, String quantity) {}
