package com.orderflow.inventory.application.usecase;

import com.orderflow.inventory.domain.model.Product;
import com.orderflow.inventory.domain.model.StockOperationType;
import com.orderflow.inventory.domain.repository.ProductRepository;
import com.orderflow.inventory.domain.repository.StockOperationRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class AdjustmentOutStockUseCase extends StockOperationUseCase {
    public AdjustmentOutStockUseCase(ProductRepository productRepository, StockOperationRepository stockOperationRepository) {
        super(productRepository, stockOperationRepository, StockOperationType.ADJUSTMENT_OUT);
    }

    @Override
    protected void applyStockChange(Product product, BigDecimal quantity) {
        product.decreaseStock(quantity);
    }
}
