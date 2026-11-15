CREATE TABLE customer_order(
    id UUID PRIMARY KEY,
    status VARCHAR(30) NOT NULL CHECK (status IN ('WAITING_PAYMENT', 'WAITING_DELIVERY', 'IN_DELIVERY', 'DELIVERED', 'FAILED', 'CANCELLED')),
    version BIGINT NOT NULL DEFAULT 0,
    total_amount NUMERIC(15, 2) NOT NULL CHECK (total_amount >= 0),
    created_at pg_catalog.timestamptz NOT NULL DEFAULT NOW(),
    updated_at pg_catalog.timestamptz NOT NULL DEFAULT NOW()
);
