package com.orderflow.order.infrastructure.persistence;

import com.orderflow.order.domain.model.OrderItem;

public class OrderItemJpaMapper {
    public static OrderItemJpaEntity toJpaEntity(OrderItem item) {
        if (item == null) return null;
        OrderItemJpaEntity entity = new OrderItemJpaEntity();
        entity.setId(item.getId());
        entity.setOrderId(item.getOrderId());
        entity.setProductId(item.getProductId());
        entity.setQuantity(item.getQuantity());
        entity.setUnitPrice(item.getUnitPrice());
        entity.setTotalAmount(item.getTotalAmount());
        entity.setCreatedAt(item.getCreatedAt());
        return entity;
    }

    public static OrderItem toDomainEntity(OrderItemJpaEntity entity) {
        if (entity == null) return null;
        return OrderItem.rehydrate(entity.getId(), entity.getOrderId(), entity.getProductId(),
                entity.getQuantity(), entity.getUnitPrice(), entity.getCreatedAt());
    }
}
