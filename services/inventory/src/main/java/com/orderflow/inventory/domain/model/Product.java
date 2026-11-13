package com.orderflow.inventory.domain.model;

import com.orderflow.inventory.domain.exception.InsufficientStockException;
import com.orderflow.inventory.domain.exception.LengthExceededException;
import com.orderflow.inventory.domain.exception.NegativePriceException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Product {
    private static final int MAX_NAME_LENGTH = 30;
    private static final int MAX_DESCRIPTION_LENGTH = 300;

    private final UUID id;
    private final Long version;
    private String name;
    private String description;
    private BigDecimal quantityInStock;
    private ProductUnit unit;
    private BigDecimal pricePerUnit;
    private ProductStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    public Product(String name, String description, BigDecimal quantityInStock,
                   ProductUnit unit, BigDecimal pricePerUnit, ProductStatus status) {
        if (name == null || quantityInStock == null
                || unit == null || pricePerUnit == null || status == null)
            throw new IllegalArgumentException("Product fields cannot be null");
        if (quantityInStock.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("Initial stock cannot be negative");
        BigDecimal normalizedQuantity = normalizeStockQuantity(quantityInStock);
        BigDecimal normalizedPrice = validatePricePerUnit(pricePerUnit);

        String normalizedDescription = normalizeDescription(description);
        String normalizedName = normalizeName(name);

        this.id = UUID.randomUUID();
        this.version = null;
        this.name = normalizedName;
        this.description = normalizedDescription;
        this.quantityInStock = normalizedQuantity;
        this.unit = unit;
        this.pricePerUnit = normalizedPrice;
        this.status = status;
        this.createdAt = Instant.now();
        this.updatedAt = createdAt;
    }

    private Product(UUID id, String name, String description, BigDecimal quantityInStock,
                    ProductUnit unit, BigDecimal pricePerUnit, ProductStatus status,
                    Instant createdAt, Instant updatedAt, Long version) {
        this.id = Objects.requireNonNull(id, "Product id cannot be null");
        this.version = version;
        this.name = normalizeName(name);
        this.description = normalizeDescription(description);
        Objects.requireNonNull(quantityInStock, "Quantity cannot be null");
        if (quantityInStock.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("Initial stock cannot be negative");
        this.quantityInStock = normalizeStockQuantity(quantityInStock);
        this.unit = Objects.requireNonNull(unit, "Unit cannot be null");
        this.pricePerUnit = validatePricePerUnit(pricePerUnit);
        this.status = Objects.requireNonNull(status, "Status cannot be null");
        this.createdAt = Objects.requireNonNull(createdAt, "Created at cannot be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "Updated at cannot be null");
    }

    public static Product rehydrate(UUID id, String name, String description,
                                    BigDecimal quantityInStock, ProductUnit unit,
                                    BigDecimal pricePerUnit, ProductStatus status,
                                    Instant createdAt, Instant updatedAt) {
        return rehydrate(id, name, description, quantityInStock, unit, pricePerUnit,
                status, createdAt, updatedAt, null);
    }

    public static Product rehydrate(UUID id, String name, String description,
                                    BigDecimal quantityInStock, ProductUnit unit,
                                    BigDecimal pricePerUnit, ProductStatus status,
                                    Instant createdAt, Instant updatedAt, Long version) {
        return new Product(id, name, description, quantityInStock, unit, pricePerUnit,
                status, createdAt, updatedAt, version);
    }



    private static String normalizeName(String name) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Name cannot be null or blank");
        String stripped = name.strip();
        if (stripped.length() > MAX_NAME_LENGTH)
            throw new LengthExceededException("Name too long");
        return stripped;
    }

    private static String normalizeDescription(String description) {
        if (description == null) return null;
        String stripped = description.strip();
        if (stripped.isEmpty()) return null;
        if (stripped.length() > MAX_DESCRIPTION_LENGTH)
            throw new LengthExceededException("Description too long");
        return stripped;
    }

    private static BigDecimal validatePricePerUnit(BigDecimal price) {
        if (price == null)
            throw new IllegalArgumentException("Price per unit cannot be null");
        if (price.compareTo(BigDecimal.ZERO) < 0)
            throw new NegativePriceException("Price per unit cannot be negative");
        return normalizePrecision(price, 15, 2, "Price per unit");
    }

    private static void validatePositiveQuantity(BigDecimal quantity) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Quantity must be greater than zero");
    }

    private static BigDecimal normalizeStockQuantity(BigDecimal quantity) {
        return normalizePrecision(quantity, 19, 3, "Stock quantity");
    }

    private static BigDecimal normalizePrecision(BigDecimal value, int precision, int scale, String field) {
        BigDecimal significantValue = value.stripTrailingZeros();
        if (significantValue.scale() > scale)
            throw new IllegalArgumentException(field + " supports at most " + scale + " decimal places");

        int integerDigits = Math.max(significantValue.precision() - significantValue.scale(), 0);
        if (integerDigits > precision - scale)
            throw new IllegalArgumentException(field + " exceeds the maximum precision of " + precision);

        return value;
    }

    private void updateTimestamp() {
        this.updatedAt = Instant.now();
    }



    public boolean hasEnoughStock(BigDecimal quantity) {
        validatePositiveQuantity(quantity);
        return quantityInStock.compareTo(normalizeStockQuantity(quantity)) >= 0;
    }

    public void decreaseStock(BigDecimal quantity) {
        validatePositiveQuantity(quantity);
        BigDecimal normalizedQuantity = normalizeStockQuantity(quantity);
        if (this.quantityInStock.compareTo(normalizedQuantity) < 0)
            throw new InsufficientStockException("Insufficient stock");

        this.quantityInStock = normalizeStockQuantity(this.quantityInStock.subtract(normalizedQuantity));
        updateTimestamp();
    }

    public void increaseStock(BigDecimal quantity) {
        validatePositiveQuantity(quantity);
        BigDecimal normalizedQuantity = normalizeStockQuantity(quantity);
        this.quantityInStock = normalizeStockQuantity(this.quantityInStock.add(normalizedQuantity));
        updateTimestamp();
    }

    public void changeName(String newName) {
        String normalized = normalizeName(newName);
        if (normalized.equals(name)) return;
        this.name = normalized;
        updateTimestamp();
    }

    public void changeDescription(String newDescription) {
        String normalized = normalizeDescription(newDescription);
        if (Objects.equals(normalized, description)) return;

        this.description = normalized;
        updateTimestamp();
    }

    public void changeUnit(ProductUnit newUnit) {
        if (newUnit == null) throw new IllegalArgumentException("Unit cannot be null");
        if (unit.equals(newUnit)) return;
        this.unit = newUnit;
        updateTimestamp();
    }

    public void changePricePerUnit(BigDecimal newPricePerUnit) {
        BigDecimal normalizedPrice = validatePricePerUnit(newPricePerUnit);
        if (pricePerUnit.compareTo(normalizedPrice) == 0) return;
        this.pricePerUnit = normalizedPrice;
        updateTimestamp();
    }

    public void activate() {
        if (this.status == ProductStatus.ACTIVE) return;
        this.status = ProductStatus.ACTIVE;
        updateTimestamp();
    }

    public void deactivate() {
        if (this.status == ProductStatus.INACTIVE) return;
        this.status = ProductStatus.INACTIVE;
        updateTimestamp();
    }

    public UUID getId() { return id; }
    public Long getVersion() { return version; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BigDecimal getQuantityInStock() { return quantityInStock; }
    public ProductUnit getUnit() { return unit; }
    public BigDecimal getPricePerUnit() { return pricePerUnit; }
    public ProductStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
