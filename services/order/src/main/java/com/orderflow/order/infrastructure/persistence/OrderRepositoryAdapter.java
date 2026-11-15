package com.orderflow.order.infrastructure.persistence;

import com.orderflow.order.domain.model.Order;
import com.orderflow.order.domain.model.OrderItem;
import com.orderflow.order.domain.model.OrderStatus;
import com.orderflow.order.domain.repository.OrderRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class OrderRepositoryAdapter implements OrderRepository {
    private static final Sort ORDER_SORT = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("id"));
    private final OrderJpaRepository repository;
    private final OrderItemJpaRepository itemRepository;

    public OrderRepositoryAdapter(OrderJpaRepository repository, OrderItemJpaRepository itemRepository) {
        this.repository = repository;
        this.itemRepository = itemRepository;
    }

    @Override
    @Transactional
    public Order save(Order order) {
        OrderJpaEntity saved = repository.saveAndFlush(OrderJpaMapper.toJpaEntity(order));
        return OrderJpaMapper.toDomainEntity(saved, order.getItems());
    }
    @Override
    @Transactional(readOnly = true)
    public Order findById(UUID id) {
        return repository.findById(id).map(entity -> OrderJpaMapper.toDomainEntity(entity,
                itemRepository.findAllByOrderId(id).stream().map(OrderItemJpaMapper::toDomainEntity).toList()))
                .orElse(null);
    }
    @Override
    @Transactional(readOnly = true)
    public List<Order> findAll(int page, int size) {
        return toOrders(repository.findAll(PageRequest.of(page, size, ORDER_SORT)).getContent());
    }
    @Override
    @Transactional(readOnly = true)
    public List<Order> findByFilters(OrderStatus status, int page, int size) {
        if (status == null) return findAll(page, size);
        return toOrders(repository.findAllByStatus(status, PageRequest.of(page, size, ORDER_SORT)).getContent());
    }
    private List<Order> toOrders(List<OrderJpaEntity> entities) {
        if (entities.isEmpty()) return List.of();
        List<UUID> ids = entities.stream().map(OrderJpaEntity::getId).toList();
        Map<UUID, List<OrderItem>> items = itemRepository.findAllByOrderIdIn(ids).stream()
                .map(OrderItemJpaMapper::toDomainEntity).collect(Collectors.groupingBy(OrderItem::getOrderId));
        return entities.stream().map(entity -> OrderJpaMapper.toDomainEntity(entity,
                items.getOrDefault(entity.getId(), List.of()))).toList();
    }
}
