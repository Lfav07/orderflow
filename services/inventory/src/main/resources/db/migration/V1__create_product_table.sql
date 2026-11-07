CREATE TABLE product(
    id UUID PRIMARY KEY,
    name VARCHAR(30) NOT NULL,
    description VARCHAR(300),
    quantity_in_stock NUMERIC(19, 3) NOT NULL CHECK (quantity_in_stock >= 0),
    unit VARCHAR(15) NOT NULL CHECK (unit IN ('GRAMS', 'KILOGRAMS', 'LITERS', 'MILLILITERS', 'UNITS')),
    price_per_unit NUMERIC(15, 2) NOT NULL CHECK (price_per_unit >= 0),
    status VARCHAR(15) NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at pg_catalog.timestamptz NOT NULL DEFAULT NOW(),
    updated_at pg_catalog.timestamptz NOT NULL DEFAULT NOW()
);
