package com.orderflow.inventory.application.command;

import com.orderflow.inventory.domain.model.ProductStatus;
import com.orderflow.inventory.domain.model.ProductUnit;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateProductCommandTest {
    private static Validator validator;
    private static jakarta.validation.ValidatorFactory validatorFactory;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void acceptsValidCommand() {
        CreateProductCommand command = validCommand();

        assertTrue(validator.validate(command).isEmpty());
    }

    @Test
    void rejectsInvalidFields() {
        CreateProductCommand command = new CreateProductCommand(
                " ", "d".repeat(301), new BigDecimal("-1"), null,
                new BigDecimal("-0.01"), null
        );

        var violations = validator.validate(command);

        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("name")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("description")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("quantityInStock")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("unit")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("pricePerUnit")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("status")));
    }

    private CreateProductCommand validCommand() {
        return new CreateProductCommand("Café", "Grãos", new BigDecimal("2.5"),
                ProductUnit.KILOGRAMS, new BigDecimal("12.00"), ProductStatus.ACTIVE);
    }
}
