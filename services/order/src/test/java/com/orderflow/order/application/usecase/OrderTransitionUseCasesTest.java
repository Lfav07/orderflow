package com.orderflow.order.application.usecase;

import com.orderflow.order.application.command.TransitionOrderCommand;
import com.orderflow.order.domain.model.*;
import com.orderflow.order.domain.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import static org.junit.jupiter.api.Assertions.*;

class OrderTransitionUseCasesTest {
    @Test void transitionsAndRejectsInvalidStates() {
        for (int action = 0; action < 5; action++) {
            for (OrderStatus state : OrderStatus.values()) {
                var repo = new RecordingRepository();
                UUID id = UUID.randomUUID();
                Instant now = Instant.now();
                repo.order = Order.rehydrate(id, state, List.of(new OrderItem(id, UUID.randomUUID(), BigDecimal.ONE, BigDecimal.ONE)), 1L, now, now);
                var command = new TransitionOrderCommand(id);
                var useCase = useCase(action, repo);
                OrderStatus target = target(action, state);
                if (target == null) {
                    assertThrows(IllegalStateException.class, () -> useCase.apply(command));
                    assertEquals(state, repo.order.getStatus());
                    assertEquals(0, repo.saves);
                } else {
                    assertSame(repo.order, useCase.apply(command));
                    assertEquals(target, repo.order.getStatus());
                    assertEquals(1, repo.saves);
                }
            }
        }
    }
    @Test void rejectsMissingInputAndOrders() {
        for (int action = 0; action < 5; action++) {
            var repo = new RecordingRepository();
            var useCase = useCase(action, repo);
            assertThrows(NullPointerException.class, () -> useCase.apply(null));
            assertThrows(NullPointerException.class, () -> useCase.apply(new TransitionOrderCommand(null)));
            assertThrows(NoSuchElementException.class, () -> useCase.apply(new TransitionOrderCommand(UUID.randomUUID())));
            assertEquals(0, repo.saves);
        }
    }
    private static Function<TransitionOrderCommand, Order> useCase(int action, OrderRepository repo) {
        return switch (action) {
            case 0 -> new MarkOrderWaitingDeliveryUseCase(repo)::execute;
            case 1 -> new StartOrderDeliveryUseCase(repo)::execute;
            case 2 -> new DeliverOrderUseCase(repo)::execute;
            case 3 -> new CancelOrderUseCase(repo)::execute;
            case 4 -> new FailOrderUseCase(repo)::execute;
            default -> throw new AssertionError();
        };
    }
    private static OrderStatus target(int action, OrderStatus state) {
        return switch (action) {
            case 0 -> state == OrderStatus.WAITING_PAYMENT ? OrderStatus.WAITING_DELIVERY : null;
            case 1 -> state == OrderStatus.WAITING_DELIVERY ? OrderStatus.IN_DELIVERY : null;
            case 2 -> state == OrderStatus.IN_DELIVERY ? OrderStatus.DELIVERED : null;
            case 3 -> state == OrderStatus.WAITING_PAYMENT ? OrderStatus.CANCELLED : null;
            case 4 -> state == OrderStatus.WAITING_PAYMENT || state == OrderStatus.WAITING_DELIVERY || state == OrderStatus.IN_DELIVERY ? OrderStatus.FAILED : null;
            default -> throw new AssertionError();
        };
    }
    private static class RecordingRepository implements OrderRepository {
        Order order; int saves;
        public Order save(Order order) { saves++; return order; }
        public Order findById(UUID id) { return order != null && order.getId().equals(id) ? order : null; }
        public List<Order> findAll(int page, int size) { throw new UnsupportedOperationException(); }
        public List<Order> findByFilters(OrderStatus status, int page, int size) { throw new UnsupportedOperationException(); }
    }
}
