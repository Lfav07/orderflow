package com.orderflow.order.domain.repository;

import com.orderflow.order.domain.model.Order;
import com.orderflow.order.domain.model.OrderStatus;

import java.util.List;
import java.util.UUID;

public interface OrderRepository {
    Order save(Order order);

    /** Returns the complete order, including its items, or null when it does not exist. */
    Order findById(UUID id);

    /** Zero-based page, sorted by createdAt descending and then id ascending before pagination. */
    List<Order> findAll(int page, int size);

    /** Applies the status filter before using the same ordering and pagination as findAll. */
    List<Order> findByFilters(OrderStatus status, int page, int size);
}
