CREATE TABLE stock_operation(
    operation_id UUID PRIMARY KEY,
    product_id UUID NOT NULL,
    quantity NUMERIC(19, 3) NOT NULL CHECK (quantity > 0),
    type VARCHAR(20) NOT NULL CHECK (type IN ('DEBIT', 'RETURN', 'ADJUSTMENT_IN', 'ADJUSTMENT_OUT')),
    created_at pg_catalog.timestamptz NOT NULL default NOW()
)
