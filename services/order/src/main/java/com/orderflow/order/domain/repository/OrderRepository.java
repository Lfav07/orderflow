package com.orderflow.order.domain.repository;

import com.orderflow.order.domain.model.Order;
import com.orderflow.order.domain.model.OrderStatus;

import java.util.List;
import java.util.UUID;

public interface OrderRepository {
    Order save(Order order);

    Order findById(UUID id);

    List<Order> findAll(int page, int size);

    List<Order> findByFilters(OrderStatus status, int page, int size);
}
