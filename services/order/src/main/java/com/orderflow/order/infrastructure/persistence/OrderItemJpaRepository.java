package com.orderflow.order.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface OrderItemJpaRepository extends JpaRepository<OrderItemJpaEntity, UUID> {
    List<OrderItemJpaEntity> findAllByOrderId(UUID orderId);
    List<OrderItemJpaEntity> findAllByOrderIdIn(Collection<UUID> orderIds);
}
