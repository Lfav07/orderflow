package com.orderflow.order.grpc;

public record CheckStockAvailabilityRequest(String productId, String quantity) {}
