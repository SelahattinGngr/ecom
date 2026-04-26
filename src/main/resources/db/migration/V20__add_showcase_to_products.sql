ALTER TABLE products ADD COLUMN is_showcase BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_products_is_showcase ON products (is_showcase) WHERE is_showcase = TRUE;
