package com.orderflow.order.application.gateway;

import com.orderflow.order.grpc.CheckStockAvailabilityRequest;
import com.orderflow.order.grpc.CheckStockAvailabilityResponse;
import com.orderflow.order.grpc.GetProductPriceRequest;
import com.orderflow.order.grpc.GetProductPriceResponse;
import com.orderflow.order.grpc.StockOperationRequest;
import com.orderflow.order.grpc.StockOperationResponse;

public interface InventoryGateway {
    StockOperationResponse debitStock(StockOperationRequest request);

    StockOperationResponse returnStock(StockOperationRequest request);

    CheckStockAvailabilityResponse checkStockAvailability(CheckStockAvailabilityRequest request);

    GetProductPriceResponse getProductPrice(GetProductPriceRequest request);
}
