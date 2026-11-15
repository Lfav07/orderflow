package com.orderflow.order.infrastructure.persistence;

import com.orderflow.order.domain.model.OrderItem;
import com.orderflow.order.domain.repository.OrderItemRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public class OrderItemRepositoryAdapter implements OrderItemRepository {
    private final OrderItemJpaRepository repository;

    public OrderItemRepositoryAdapter(OrderItemJpaRepository repository) { this.repository = repository; }

    @Override
    public OrderItem save(OrderItem item) {
        return OrderItemJpaMapper.toDomainEntity(repository.save(OrderItemJpaMapper.toJpaEntity(item)));
    }
    @Override
    public List<OrderItem> saveAll(List<OrderItem> items) {
        return repository.saveAll(items.stream().map(OrderItemJpaMapper::toJpaEntity).toList())
                .stream().map(OrderItemJpaMapper::toDomainEntity).toList();
    }
    @Override
    public OrderItem findById(UUID id) {
        return repository.findById(id).map(OrderItemJpaMapper::toDomainEntity).orElse(null);
    }
    @Override
    public List<OrderItem> findAllByOrderId(UUID orderId) {
        return repository.findAllByOrderId(orderId).stream().map(OrderItemJpaMapper::toDomainEntity).toList();
    }
}
