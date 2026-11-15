package com.orderflow.order.infrastructure.persistence;

import com.orderflow.order.domain.model.Order;
import com.orderflow.order.domain.model.OrderItem;
import java.util.List;

public class OrderJpaMapper {
    public static OrderJpaEntity toJpaEntity(Order order) {
        if (order == null) return null;
        OrderJpaEntity entity = new OrderJpaEntity();
        entity.setId(order.getId());
        entity.setStatus(order.getStatus());
        entity.setVersion(order.getVersion());
        entity.setTotalAmount(order.getTotalAmount());
        entity.setCreatedAt(order.getCreatedAt());
        entity.setUpdatedAt(order.getUpdatedAt());
        return entity;
    }

    public static Order toDomainEntity(OrderJpaEntity entity, List<OrderItem> items) {
        if (entity == null) return null;
        return Order.rehydrate(entity.getId(), entity.getStatus(), items, entity.getVersion(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
