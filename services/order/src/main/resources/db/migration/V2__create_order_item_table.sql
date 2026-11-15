CREATE TABLE order_item(
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES customer_order(id),
    product_id UUID NOT NULL,
    quantity NUMERIC(19, 3) NOT NULL CHECK (quantity > 0),
    unit_price NUMERIC(15, 2) NOT NULL CHECK (unit_price >= 0),
    total_amount NUMERIC(15, 2) NOT NULL CHECK (total_amount >= 0),
    created_at pg_catalog.timestamptz NOT NULL DEFAULT NOW()
);
