package com.orderflow.inventory.application.usecase;

import com.orderflow.inventory.domain.model.Product;
import com.orderflow.inventory.domain.model.StockOperationType;
import com.orderflow.inventory.domain.repository.ProductRepository;
import com.orderflow.inventory.domain.repository.StockOperationRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DebitStockUseCase extends StockOperationUseCase {
    public DebitStockUseCase(ProductRepository productRepository, StockOperationRepository stockOperationRepository) {
        super(productRepository, stockOperationRepository, StockOperationType.DEBIT);
    }

    @Override
    protected void applyStockChange(Product product, BigDecimal quantity) {
        product.decreaseStock(quantity);
    }
}
