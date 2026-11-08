package com.orderflow.inventory.infrastructure.grpc;

import com.orderflow.inventory.domain.model.StockOperation;
import com.orderflow.inventory.domain.model.StockOperationType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class StockGrpcMapperTest {
    @Test
    void requestMappingPreservesIdentifiersAndDecimalQuantity() {
        UUID operationId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        var request = com.orderflow.inventory.grpc.StockOperationRequest.newBuilder()
                .setOperationId(operationId.toString()).setProductId(productId.toString()).setQuantity("1.250").build();

        var command = StockGrpcMapper.toStockOperationCommand(request);

        assertEquals(operationId, command.operationId());
        assertEquals(productId, command.productId());
        assertEquals(new BigDecimal("1.250"), command.quantity());
    }

    @Test
    void responseMappingPreservesAllOperationValuesAndTypeNames() {
        for (StockOperationType type : StockOperationType.values()) {
            UUID operationId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();
            Instant createdAt = Instant.parse("2026-01-02T03:04:05Z");
            StockOperation operation = StockOperation.rehydrate(operationId, productId, new BigDecimal("2.500"), type, createdAt);

            var response = StockGrpcMapper.toStockOperationResponse(operation);

            assertEquals(operationId.toString(), response.getOperationId());
            assertEquals(productId.toString(), response.getProductId());
            assertEquals("2.500", response.getQuantity());
            assertEquals(type.name(), response.getType().name());
            assertEquals(createdAt.toString(), response.getCreatedAt());
        }
    }

    @Test
    void requestMappingRejectsMalformedUuidAndDecimal() {
        var malformedId = com.orderflow.inventory.grpc.StockOperationRequest.newBuilder()
                .setOperationId("bad").setProductId(UUID.randomUUID().toString()).setQuantity("1").build();
        var malformedQuantity = com.orderflow.inventory.grpc.StockOperationRequest.newBuilder()
                .setOperationId(UUID.randomUUID().toString()).setProductId(UUID.randomUUID().toString()).setQuantity("one").build();

        assertThrows(IllegalArgumentException.class, () -> StockGrpcMapper.toStockOperationCommand(malformedId));
        assertThrows(NumberFormatException.class, () -> StockGrpcMapper.toStockOperationCommand(malformedQuantity));
    }
}
