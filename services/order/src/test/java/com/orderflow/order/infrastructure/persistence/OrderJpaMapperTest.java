package com.orderflow.order.infrastructure.persistence;

import com.orderflow.order.domain.model.Order;
import com.orderflow.order.domain.model.OrderItem;
import com.orderflow.order.domain.model.OrderStatus;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class OrderJpaMapperTest {
    @Test
    void mappersPreserveAggregateAndPersistenceMetadata() {
        UUID orderId = UUID.randomUUID();
        Instant created = Instant.parse("2025-01-01T00:00:00Z");
        OrderItem item = OrderItem.rehydrate(UUID.randomUUID(), orderId, UUID.randomUUID(),
                new BigDecimal("1.500"), new BigDecimal("2.50"), created);
        Order original = Order.rehydrate(orderId, OrderStatus.IN_DELIVERY, List.of(item), 2L, created, created.plusSeconds(1));
        OrderJpaEntity orderEntity = OrderJpaMapper.toJpaEntity(original);
        OrderItemJpaEntity itemEntity = OrderItemJpaMapper.toJpaEntity(item);
        Order result = OrderJpaMapper.toDomainEntity(orderEntity, List.of(OrderItemJpaMapper.toDomainEntity(itemEntity)));
        assertEquals(original.getId(), result.getId());
        assertEquals(original.getStatus(), result.getStatus());
        assertEquals(original.getVersion(), result.getVersion());
        assertEquals(original.getCreatedAt(), result.getCreatedAt());
        assertEquals(original.getUpdatedAt(), result.getUpdatedAt());
        assertEquals(item.getId(), result.getItems().getFirst().getId());
        assertEquals(item.getProductId(), result.getItems().getFirst().getProductId());
        assertEquals(0, original.getTotalAmount().compareTo(result.getTotalAmount()));
        assertEquals(0, original.getTotalAmount().compareTo(orderEntity.getTotalAmount()));
        assertEquals(0, item.getTotalAmount().compareTo(itemEntity.getTotalAmount()));
    }
}
