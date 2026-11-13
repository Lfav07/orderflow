package com.orderflow.order.application.usecase;

import com.orderflow.order.application.command.CreateOrderItemCommand;
import com.orderflow.order.application.gateway.InsufficientStockException;
import com.orderflow.order.application.gateway.InventoryGateway;
import com.orderflow.order.domain.model.OrderItem;
import com.orderflow.order.grpc.CheckStockAvailabilityRequest;
import com.orderflow.order.grpc.GetProductPriceRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Objects;

@Component
public class CreateOrderItemUseCase {
    private final InventoryGateway inventoryGateway;
    public CreateOrderItemUseCase(InventoryGateway inventoryGateway) {
        this.inventoryGateway = Objects.requireNonNull(inventoryGateway, "Inventory gateway cannot be null");
    }
    public OrderItem execute(CreateOrderItemCommand command) {
        Objects.requireNonNull(command, "Command cannot be null");
        Objects.requireNonNull(command.orderId(), "Order id cannot be null");
        Objects.requireNonNull(command.productId(), "Product id cannot be null");
        Objects.requireNonNull(command.quantity(), "Quantity cannot be null");
        if (command.quantity().signum() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        CheckStockAvailabilityRequest request = new CheckStockAvailabilityRequest(command.productId().toString(), command.quantity().toPlainString());
        boolean hasStock = inventoryGateway.checkStockAvailability(request).available();
        if (!hasStock) {
            throw new InsufficientStockException("Insufficient stock for product " + command.productId());
        }
        GetProductPriceRequest productPriceRequest = new GetProductPriceRequest(command.productId().toString());
        BigDecimal currentPricePerUnit = new BigDecimal(inventoryGateway.getProductPrice(productPriceRequest).unitPrice());
        return new OrderItem(command.orderId(), command.productId(), command.quantity(), currentPricePerUnit);
    }
}
