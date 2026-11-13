package com.orderflow.order.grpc;

public record CheckStockAvailabilityResponse(String productId, boolean available) {}
