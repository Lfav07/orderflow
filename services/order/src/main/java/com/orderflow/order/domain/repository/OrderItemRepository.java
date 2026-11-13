package com.orderflow.order.domain.repository;

import com.orderflow.order.domain.model.OrderItem;

import java.util.List;
import java.util.UUID;

public interface OrderItemRepository {
    OrderItem findById(UUID id);

    List<OrderItem> findAllByOrderId(UUID orderId);
}
