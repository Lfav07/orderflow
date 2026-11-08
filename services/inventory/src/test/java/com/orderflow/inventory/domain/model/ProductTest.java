package com.orderflow.inventory.domain.model;

import com.orderflow.inventory.domain.exception.InsufficientStockException;
import com.orderflow.inventory.domain.exception.LengthExceededException;
import com.orderflow.inventory.domain.exception.NegativePriceException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ProductTest {
    @Test
    void createsProductWithNormalizedTextAndInitialTimestamps() {
        Product product = product("  Café  ", "  Grãos especiais  ", "2.50", "12.00", ProductStatus.ACTIVE);

        assertNotNull(product.getId());
        assertEquals("Café", product.getName());
        assertEquals("Grãos especiais", product.getDescription());
        assertEquals(new BigDecimal("2.50"), product.getQuantityInStock());
        assertEquals(ProductUnit.KILOGRAMS, product.getUnit());
        assertEquals(new BigDecimal("12.00"), product.getPricePerUnit());
        assertEquals(ProductStatus.ACTIVE, product.getStatus());
        assertNotNull(product.getCreatedAt());
        assertEquals(product.getCreatedAt(), product.getUpdatedAt());
    }

    @Test
    void normalizesNullOrBlankDescriptionAndAllowsZeroInitialStockAndPrice() {
        Product product = product("Café", null, "0", "0", ProductStatus.INACTIVE);
        Product blankDescription = product("Café", "   ", "0", "0", ProductStatus.INACTIVE);

        assertNull(product.getDescription());
        assertNull(blankDescription.getDescription());
        assertEquals(blankDescription.getCreatedAt(), blankDescription.getUpdatedAt());
        assertEquals(BigDecimal.ZERO, product.getQuantityInStock());
        assertEquals(BigDecimal.ZERO, product.getPricePerUnit());
    }

    @Test
    void rejectsNullRequiredFields() {
        assertThrows(IllegalArgumentException.class, () -> new Product(null, null, BigDecimal.ZERO,
                ProductUnit.UNITS, BigDecimal.ZERO, ProductStatus.ACTIVE));
        assertThrows(IllegalArgumentException.class, () -> new Product("Item", null, null,
                ProductUnit.UNITS, BigDecimal.ZERO, ProductStatus.ACTIVE));
        assertThrows(IllegalArgumentException.class, () -> new Product("Item", null, BigDecimal.ZERO,
                null, BigDecimal.ZERO, ProductStatus.ACTIVE));
        assertThrows(IllegalArgumentException.class, () -> new Product("Item", null, BigDecimal.ZERO,
                ProductUnit.UNITS, null, ProductStatus.ACTIVE));
        assertThrows(IllegalArgumentException.class, () -> new Product("Item", null, BigDecimal.ZERO,
                ProductUnit.UNITS, BigDecimal.ZERO, null));
    }

    @Test
    void rejectsBlankOrOverlongName() {
        assertThrows(IllegalArgumentException.class,
                () -> product(" \t ", null, "0", "0", ProductStatus.ACTIVE));
        assertThrows(LengthExceededException.class,
                () -> product("a".repeat(31), null, "0", "0", ProductStatus.ACTIVE));
    }

    @Test
    void rejectsOverlongDescriptionAndNegativeInitialStockOrPrice() {
        assertThrows(LengthExceededException.class,
                () -> product("Item", "d".repeat(301), "0", "0", ProductStatus.ACTIVE));
        assertThrows(IllegalArgumentException.class,
                () -> product("Item", null, "-0.01", "0", ProductStatus.ACTIVE));
        assertThrows(NegativePriceException.class,
                () -> product("Item", null, "0", "-0.01", ProductStatus.ACTIVE));
    }

    @Test
    void enforcesStockAndPricePrecisionAndScaleBoundaries() {
        Product atMaximumPrecision = product("Item", null,
                "9999999999999999.999", "9999999999999.99", ProductStatus.ACTIVE);

        assertEquals(new BigDecimal("9999999999999999.999"), atMaximumPrecision.getQuantityInStock());
        assertEquals(new BigDecimal("9999999999999.99"), atMaximumPrecision.getPricePerUnit());

        assertThrows(IllegalArgumentException.class,
                () -> product("Item", null, "10000000000000000", "0", ProductStatus.ACTIVE));
        assertThrows(IllegalArgumentException.class,
                () -> product("Item", null, "1.0001", "0", ProductStatus.ACTIVE));
        assertThrows(IllegalArgumentException.class,
                () -> product("Item", null, "0", "10000000000000.00", ProductStatus.ACTIVE));
        assertThrows(IllegalArgumentException.class,
                () -> product("Item", null, "0", "0.001", ProductStatus.ACTIVE));
    }

    @Test
    void rehydratesProductWithoutChangingItsIdentityOrTimestamps() {
        var id = java.util.UUID.randomUUID();
        Instant createdAt = Instant.parse("2025-01-02T03:04:05Z");
        Instant updatedAt = Instant.parse("2025-02-03T04:05:06Z");

        Product product = Product.rehydrate(id, "  Item  ", "  Description  ",
                new BigDecimal("3.250"), ProductUnit.UNITS, new BigDecimal("4.50"),
                ProductStatus.INACTIVE, createdAt, updatedAt);

        assertEquals(id, product.getId());
        assertEquals("Item", product.getName());
        assertEquals("Description", product.getDescription());
        assertEquals(new BigDecimal("3.250"), product.getQuantityInStock());
        assertEquals(ProductStatus.INACTIVE, product.getStatus());
        assertEquals(createdAt, product.getCreatedAt());
        assertEquals(updatedAt, product.getUpdatedAt());
    }

    @Test
    void rejectsInvalidPersistedProductStateDuringRehydration() {
        var id = java.util.UUID.randomUUID();
        Instant timestamp = Instant.parse("2025-01-02T03:04:05Z");

        assertThrows(IllegalArgumentException.class, () -> Product.rehydrate(id, "Item", null,
                new BigDecimal("-1"), ProductUnit.UNITS, BigDecimal.ZERO, ProductStatus.ACTIVE,
                timestamp, timestamp));
        assertThrows(NullPointerException.class, () -> Product.rehydrate(id, "Item", null,
                BigDecimal.ONE, ProductUnit.UNITS, BigDecimal.ZERO, ProductStatus.ACTIVE,
                null, timestamp));
    }

    @Test
    void decreasesAndIncreasesStockAndUpdatesTimestamp() {
        Product product = product("Item", null, "5", "2", ProductStatus.ACTIVE);
        Instant originalUpdatedAt = product.getUpdatedAt();

        product.decreaseStock(new BigDecimal("2"));
        assertEquals(new BigDecimal("3"), product.getQuantityInStock());
        assertTrue(product.getUpdatedAt().compareTo(originalUpdatedAt) >= 0);

        product.increaseStock(new BigDecimal("1.5"));
        assertEquals(new BigDecimal("4.5"), product.getQuantityInStock());
    }

    @Test
    void rejectsInvalidStockChangesAndInsufficientStock() {
        Product product = product("Item", null, "1", "2", ProductStatus.ACTIVE);

        assertThrows(InsufficientStockException.class, () -> product.decreaseStock(new BigDecimal("1.01")));
        assertThrows(IllegalArgumentException.class, () -> product.decreaseStock(BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () -> product.decreaseStock(new BigDecimal("-1")));
        assertThrows(IllegalArgumentException.class, () -> product.decreaseStock(null));
        assertThrows(IllegalArgumentException.class, () -> product.increaseStock(BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () -> product.increaseStock(null));
        assertEquals(BigDecimal.ONE, product.getQuantityInStock());
    }

    @Test
    void allowsExactStockDepletionAndRejectsOverflowWithoutChangingStock() {
        Product product = product("Item", null, "2.125", "1", ProductStatus.ACTIVE);

        product.decreaseStock(new BigDecimal("2.125"));

        assertEquals(0, product.getQuantityInStock().compareTo(BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> product.increaseStock(new BigDecimal("10000000000000000")));
        assertEquals(0, product.getQuantityInStock().compareTo(BigDecimal.ZERO));
    }

    @Test
    void changesProductAttributesAndNormalizesDescription() {
        Product product = product("Item", "Descrição", "1", "2", ProductStatus.INACTIVE);

        product.changeName("  Novo nome  ");
        product.changeDescription("  Nova descrição  ");
        product.changeUnit(ProductUnit.LITERS);
        product.changePricePerUnit(new BigDecimal("3.50"));
        product.activate();

        assertEquals("Novo nome", product.getName());
        assertEquals("Nova descrição", product.getDescription());
        assertEquals(ProductUnit.LITERS, product.getUnit());
        assertEquals(new BigDecimal("3.50"), product.getPricePerUnit());
        assertEquals(ProductStatus.ACTIVE, product.getStatus());

        product.changeDescription("   ");
        assertNull(product.getDescription());
        Instant beforeEquivalentDescription = product.getUpdatedAt();
        product.changeDescription(null);
        assertNull(product.getDescription());
        assertEquals(beforeEquivalentDescription, product.getUpdatedAt());
    }

    @Test
    void rejectsInvalidAttributeChanges() {
        Product product = product("Item", null, "1", "2", ProductStatus.ACTIVE);

        assertThrows(IllegalArgumentException.class, () -> product.changeName("  "));
        assertThrows(LengthExceededException.class, () -> product.changeName("x".repeat(31)));
        assertThrows(LengthExceededException.class, () -> product.changeDescription("x".repeat(301)));
        assertThrows(IllegalArgumentException.class, () -> product.changeUnit(null));
        assertThrows(NegativePriceException.class, () -> product.changePricePerUnit(new BigDecimal("-0.01")));
        assertThrows(IllegalArgumentException.class, () -> product.changePricePerUnit(null));
    }

    @Test
    void repeatedChangesToSameValuePreserveUpdatedTimestamp() {
        Product product = product("Item", "Descrição", "1", "2.00", ProductStatus.ACTIVE);
        Instant originalUpdatedAt = product.getUpdatedAt();

        product.changeName(" Item ");
        product.changeDescription("Descrição");
        product.changeUnit(ProductUnit.KILOGRAMS);
        product.changePricePerUnit(new BigDecimal("2.0"));
        product.activate();
        product.deactivate();
        product.deactivate();

        assertEquals(ProductStatus.INACTIVE, product.getStatus());
        assertEquals(originalUpdatedAt, product.getUpdatedAt());
    }

    private Product product(String name, String description, String quantity, String price, ProductStatus status) {
        return new Product(name, description, new BigDecimal(quantity), ProductUnit.KILOGRAMS,
                new BigDecimal(price), status);
    }
}
