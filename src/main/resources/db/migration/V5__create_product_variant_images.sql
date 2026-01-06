CREATE INDEX IF NOT EXISTS idx_images_product_order
    ON product_images (product_id, display_order)
    WHERE deleted_at IS NULL;
    
CREATE TABLE IF NOT EXISTS product_variant_images
(
    id                 UUID PRIMARY KEY     DEFAULT gen_random_uuid(),

    product_variant_id UUID        NOT NULL
        REFERENCES product_variants (id) ON DELETE CASCADE,

    url                TEXT        NOT NULL,
    display_order      INT         NOT NULL DEFAULT 0,
    is_thumbnail       BOOLEAN     NOT NULL DEFAULT FALSE,

    created_at         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at         TIMESTAMPTZ
);
CREATE INDEX idx_variant_images_variant_id
    ON product_variant_images (product_variant_id);
CREATE UNIQUE INDEX ux_variant_images_one_thumbnail
    ON product_variant_images (product_variant_id)
    WHERE is_thumbnail = TRUE AND deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_variant_images_variant_order
    ON product_variant_images (product_variant_id, display_order)
    WHERE deleted_at IS NULL;