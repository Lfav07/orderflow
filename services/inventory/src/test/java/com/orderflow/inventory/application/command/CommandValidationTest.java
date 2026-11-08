package com.orderflow.inventory.application.command;

import com.orderflow.inventory.domain.model.ProductStatus;
import com.orderflow.inventory.domain.model.ProductUnit;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CommandValidationTest {
    private static Validator validator;
    private static jakarta.validation.ValidatorFactory factory;

    @BeforeAll
    static void createValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() { factory.close(); }

    @Test
    void productCreationCommandValidatesRequiredTextAndNonnegativeAmounts() {
        assertTrue(validator.validate(new CreateProductCommand("Coffee", null, BigDecimal.ZERO,
                ProductUnit.UNITS, BigDecimal.ZERO, ProductStatus.ACTIVE)).isEmpty());
        assertFalse(validator.validate(new CreateProductCommand("  ", null, BigDecimal.ZERO,
                ProductUnit.UNITS, BigDecimal.ZERO, ProductStatus.ACTIVE)).isEmpty());
        assertFalse(validator.validate(new CreateProductCommand("x".repeat(31), null, BigDecimal.ONE,
                ProductUnit.UNITS, BigDecimal.ONE, ProductStatus.ACTIVE)).isEmpty());
        assertFalse(validator.validate(new CreateProductCommand("Coffee", "x".repeat(301), BigDecimal.ONE,
                ProductUnit.UNITS, BigDecimal.ONE, ProductStatus.ACTIVE)).isEmpty());
        assertFalse(validator.validate(new CreateProductCommand("Coffee", null, new BigDecimal("-0.01"),
                ProductUnit.UNITS, BigDecimal.ONE, ProductStatus.ACTIVE)).isEmpty());
    }

    @Test
    void stockCommandsRequireIdentifiersAndStrictlyPositiveQuantity() {
        assertTrue(validator.validate(new ChangeStockCommand(UUID.randomUUID(), BigDecimal.ONE)).isEmpty());
        assertFalse(validator.validate(new ChangeStockCommand(null, BigDecimal.ONE)).isEmpty());
        assertFalse(validator.validate(new ChangeStockCommand(UUID.randomUUID(), BigDecimal.ZERO)).isEmpty());
        assertFalse(validator.validate(new StockOperationCommand(UUID.randomUUID(), UUID.randomUUID(), BigDecimal.ZERO)).isEmpty());
        assertFalse(validator.validate(new StockOperationCommand(null, UUID.randomUUID(), BigDecimal.ONE)).isEmpty());
    }

    @Test
    void priceCommandAllowsZeroButRejectsNullAndNegativeValues() {
        assertTrue(validator.validate(new ChangeProductPriceCommand(UUID.randomUUID(), BigDecimal.ZERO)).isEmpty());
        assertFalse(validator.validate(new ChangeProductPriceCommand(UUID.randomUUID(), null)).isEmpty());
        assertFalse(validator.validate(new ChangeProductPriceCommand(UUID.randomUUID(), new BigDecimal("-0.01"))).isEmpty());
    }

    @Test
    void paginationCommandsValidateNonnegativePageAndPositiveSize() {
        assertTrue(validator.validate(new ListProductsCommand(0, 1)).isEmpty());
        assertFalse(validator.validate(new ListProductsCommand(-1, 1)).isEmpty());
        assertFalse(validator.validate(new ListProductsCommand(0, 0)).isEmpty());
        assertFalse(validator.validate(new FilterProductsCommand(null, null, null, 0, 0)).isEmpty());
        assertFalse(validator.validate(new ListStockOperationsCommand(null, null, -1, 5)).isEmpty());
    }

    @Test
    void attributeCommandsValidateRequiredValuesAndLengthLimits() {
        assertTrue(validator.validate(new ChangeProductNameCommand(UUID.randomUUID(), "Tea")).isEmpty());
        assertFalse(validator.validate(new ChangeProductNameCommand(UUID.randomUUID(), "")).isEmpty());
        assertFalse(validator.validate(new ChangeProductNameCommand(UUID.randomUUID(), "x".repeat(31))).isEmpty());
        assertFalse(validator.validate(new ChangeProductDescriptionCommand(UUID.randomUUID(), "x".repeat(301))).isEmpty());
        assertFalse(validator.validate(new ChangeProductUnitCommand(null, ProductUnit.UNITS)).isEmpty());
        assertFalse(validator.validate(new GetProductByIdCommand(null)).isEmpty());
        assertFalse(validator.validate(new GetStockOperationByIdCommand(null)).isEmpty());
        assertFalse(validator.validate(new ProductIdCommand(null)).isEmpty());
    }
}
