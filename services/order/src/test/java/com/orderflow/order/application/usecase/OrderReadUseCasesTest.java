package com.orderflow.order.application.usecase;

import com.orderflow.order.application.command.*;
import com.orderflow.order.domain.model.*;
import com.orderflow.order.domain.repository.OrderRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OrderReadUseCasesTest {
    private final RecordingRepository repository = new RecordingRepository();

    @Test
    void retrievesCompleteOrderAndPreservesMissingResult() {
        UUID id = UUID.randomUUID();
        Instant created = Instant.parse("2025-01-01T00:00:00Z");
        OrderItem item = OrderItem.rehydrate(UUID.randomUUID(), id, UUID.randomUUID(),
                new BigDecimal("2"), new BigDecimal("3.50"), created);
        Order order = Order.rehydrate(id, OrderStatus.DELIVERED, List.of(item), 1L, created, created.plusSeconds(10));
        repository.order = order;

        Order result = new GetOrderByIdUseCase(repository).execute(new GetOrderByIdCommand(id));
        assertEquals(OrderStatus.DELIVERED, result.getStatus());
        assertEquals(new BigDecimal("3.50"), result.getItems().getFirst().getUnitPrice());
        assertEquals(0, new BigDecimal("7.00").compareTo(result.getTotalAmount()));
        assertEquals(created, result.getCreatedAt());
        assertEquals(created.plusSeconds(10), result.getUpdatedAt());
        UUID missing = UUID.randomUUID();
        assertNull(new GetOrderByIdUseCase(repository).execute(new GetOrderByIdCommand(missing)));
        assertThrows(NullPointerException.class, () -> new GetOrderByIdUseCase(repository).execute(new GetOrderByIdCommand(null)));
    }

    @Test
    void listsAndFiltersUsingRequestedPages() {
        ListOrdersUseCase list = new ListOrdersUseCase(repository);
        FilterOrdersUseCase filter = new FilterOrdersUseCase(repository);
        list.execute(new ListOrdersCommand(0, 100));
        filter.execute(new FilterOrdersCommand(OrderStatus.IN_DELIVERY, 2, 1));
        assertEquals(List.of("all:0:100", "filter:IN_DELIVERY:2:1"), repository.calls);
        assertEquals(new ListOrdersCommand(0, 20), new ListOrdersCommand());
        assertEquals(new FilterOrdersCommand(OrderStatus.CANCELLED, 0, 20), new FilterOrdersCommand(OrderStatus.CANCELLED));
    }

    @Test
    void rejectsInvalidPaginationAndMissingStatusWithoutQuerying() {
        ListOrdersUseCase list = new ListOrdersUseCase(repository);
        FilterOrdersUseCase filter = new FilterOrdersUseCase(repository);
        for (int[] page : new int[][] {{-1, 1}, {0, 0}, {0, 101}}) {
            assertThrows(IllegalArgumentException.class, () -> list.execute(new ListOrdersCommand(page[0], page[1])));
            assertThrows(IllegalArgumentException.class, () -> filter.execute(new FilterOrdersCommand(OrderStatus.FAILED, page[0], page[1])));
        }
        assertThrows(NullPointerException.class, () -> filter.execute(new FilterOrdersCommand(null, 0, 20)));
        assertTrue(repository.calls.isEmpty());
    }
    private static class RecordingRepository implements OrderRepository {
        Order order;
        final java.util.ArrayList<String> calls = new java.util.ArrayList<>();
        public Order save(Order order) { throw new UnsupportedOperationException(); }
        public Order findById(UUID id) { return order != null && order.getId().equals(id) ? order : null; }
        public List<Order> findAll(int page, int size) {
            calls.add("all:" + page + ":" + size);
            return List.of();
        }
        public List<Order> findByFilters(OrderStatus status, int page, int size) {
            calls.add("filter:" + status + ":" + page + ":" + size);
            return List.of();
        }
    }
}
