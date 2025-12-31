CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS citext;

CREATE TYPE order_status AS ENUM (
    'PENDING',
    'PAID',
    'PREPARING',
    'SHIPPED',
    'DELIVERED',
    'CANCELLED',
    'RETURNED'
    );
CREATE TYPE payment_status AS ENUM (
    'PENDING',
    'REQUIRES_ACTION',
    'SUCCEEDED',
    'FAILED',
    'CANCELLED',
    'REFUNDED'
    );
CREATE TYPE payment_provider AS ENUM (
    'STRIPE',
    'IYZICO',
    'GARANTI'
    );
CREATE TYPE refund_status AS ENUM (
    'PENDING',
    'SUCCEEDED',
    'FAILED',
    'CANCELLED'
    );

CREATE TABLE IF NOT EXISTS users
(
    id                       UUID PRIMARY KEY     DEFAULT gen_random_uuid(),

    first_name               TEXT,
    last_name                TEXT,

    email                    CITEXT      NOT NULL,
    email_verified_at        TIMESTAMPTZ,

    phone_number             TEXT        NOT NULL,
    phone_number_verified_at TIMESTAMPTZ,

    created_at               TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at               TIMESTAMPTZ
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_users_email_active
    ON users (email)
    WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS ux_users_phone_number_active
    ON users (phone_number)
    WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS roles
(
    id          UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    name        TEXT        NOT NULL UNIQUE,
    description TEXT,
    is_system   BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS permissions
(
    id          UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    name        TEXT        NOT NULL UNIQUE,
    description TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS role_permissions
(
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role_id       UUID NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions (id) ON DELETE CASCADE,
    UNIQUE (role_id, permission_id)
);
CREATE INDEX IF NOT EXISTS idx_role_permissions_role
    ON role_permissions (role_id);
CREATE INDEX IF NOT EXISTS idx_role_permissions_permission
    ON role_permissions (permission_id);

CREATE TABLE IF NOT EXISTS user_roles
(
    id      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    UNIQUE (user_id, role_id)
);
CREATE INDEX IF NOT EXISTS idx_user_roles_user_id
    ON user_roles (user_id);
CREATE INDEX IF NOT EXISTS idx_user_roles_role_id
    ON user_roles (role_id);

CREATE TABLE IF NOT EXISTS admin_audit_logs
(
    id          UUID PRIMARY KEY     DEFAULT gen_random_uuid(),

    user_id     UUID        REFERENCES users (id) ON DELETE SET NULL,

    action      TEXT        NOT NULL,
    entity_type TEXT,
    entity_id   UUID,
    metadata    JSONB,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS countries
(
    id   SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS cities
(
    id         SERIAL PRIMARY KEY,
    country_id INT          NOT NULL REFERENCES countries (id) ON DELETE RESTRICT,
    name       VARCHAR(100) NOT NULL,
    UNIQUE (country_id, name)
);
CREATE INDEX IF NOT EXISTS idx_cities_country_id ON cities (country_id);
CREATE UNIQUE INDEX IF NOT EXISTS ux_cities_id_country
    ON cities (id, country_id);

CREATE TABLE IF NOT EXISTS districts
(
    id      SERIAL PRIMARY KEY,
    city_id INT          NOT NULL REFERENCES cities (id) ON DELETE RESTRICT,
    name    VARCHAR(100) NOT NULL,
    UNIQUE (city_id, name)
);
CREATE INDEX IF NOT EXISTS idx_districts_city_id ON districts (city_id);
CREATE UNIQUE INDEX IF NOT EXISTS ux_districts_id_city
    ON districts (id, city_id);

CREATE TABLE IF NOT EXISTS addresses
(
    id           UUID PRIMARY KEY,

    user_id      UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,

    country_id   INT         NOT NULL,
    city_id      INT         NOT NULL,
    district_id  INT,
    
    neighborhood TEXT,
    street       TEXT,
    building_no  TEXT,
    apartment_no TEXT,
    postal_code  TEXT,

    title        TEXT,
    full_address TEXT,

    contact_name  TEXT,       -- EKLENDİ (Teslim Alacak Kişi)
    contact_phone TEXT,       -- EKLENDİ (İletişim Numarası)

    created_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at   TIMESTAMPTZ,

    CONSTRAINT addresses_country_fk
        FOREIGN KEY (country_id)
            REFERENCES countries (id) ON DELETE RESTRICT,
    CONSTRAINT addresses_city_country_fk
        FOREIGN KEY (city_id, country_id)
            REFERENCES cities (id, country_id) ON DELETE RESTRICT,
    CONSTRAINT addresses_district_city_fk
        FOREIGN KEY (district_id, city_id)
            REFERENCES districts (id, city_id) ON DELETE RESTRICT
);
CREATE INDEX IF NOT EXISTS idx_addresses_user_id ON addresses (user_id);
CREATE INDEX IF NOT EXISTS idx_addresses_country_id ON addresses (country_id);
CREATE INDEX IF NOT EXISTS idx_addresses_city_id ON addresses (city_id);
CREATE INDEX IF NOT EXISTS idx_addresses_district_id ON addresses (district_id);

CREATE TABLE IF NOT EXISTS categories
(
    id         SERIAL PRIMARY KEY,

    name       TEXT        NOT NULL,
    slug       TEXT        NOT NULL CHECK (length(trim(slug)) > 0),
    parent_id  INT         REFERENCES categories (id) ON DELETE SET NULL,
    image_url  TEXT,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_categories_parent_id ON categories (parent_id);
CREATE UNIQUE INDEX IF NOT EXISTS ux_categories_slug_active ON categories (slug) WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS products
(
    id          UUID PRIMARY KEY        DEFAULT gen_random_uuid(),

    category_id INT            REFERENCES categories (id) ON DELETE SET NULL,

    name        TEXT           NOT NULL,
    slug        TEXT           NOT NULL CHECK (length(trim(slug)) > 0),
    description TEXT,
    base_price  NUMERIC(10, 2) NOT NULL,

    created_at  TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at  TIMESTAMPTZ,

    CONSTRAINT chk_products_base_price_nonneg CHECK (base_price >= 0)
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_products_slug_active ON products (slug) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_products_category_id ON products (category_id);

CREATE TABLE IF NOT EXISTS product_variants
(
    id             UUID PRIMARY KEY        DEFAULT gen_random_uuid(),

    product_id     UUID           NOT NULL REFERENCES products (id) ON DELETE CASCADE,

    sku            TEXT           NOT NULL,
    size           TEXT,
    color          TEXT,
    price          NUMERIC(10, 2) NOT NULL CHECK (price >= 0),
    stock_quantity INT            NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0),
    is_active      BOOLEAN        NOT NULL DEFAULT TRUE,

    created_at     TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at     TIMESTAMPTZ
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_variants_sku_active
    ON product_variants (sku)
    WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_variants_product_id
    ON product_variants (product_id);
CREATE UNIQUE INDEX IF NOT EXISTS ux_variant_unique_combo
    ON product_variants (product_id, COALESCE(size, ''), COALESCE(color, ''))
    WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS product_images
(
    id            UUID PRIMARY KEY     DEFAULT gen_random_uuid(),

    product_id    UUID        NOT NULL REFERENCES products (id) ON DELETE CASCADE,

    url           TEXT        NOT NULL,
    display_order INT         NOT NULL DEFAULT 0,
    is_thumbnail  BOOLEAN     NOT NULL DEFAULT FALSE,

    created_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at    TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_images_product_id
    ON product_images (product_id);
CREATE UNIQUE INDEX IF NOT EXISTS ux_images_one_thumbnail_per_product
    ON product_images (product_id)
    WHERE is_thumbnail = TRUE AND deleted_at IS NULL;


CREATE TABLE IF NOT EXISTS carts
(
    id         UUID PRIMARY KEY     DEFAULT gen_random_uuid(),

    user_id    UUID        NOT NULL REFERENCES users (id),

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ
);
CREATE UNIQUE INDEX IF NOT EXISTS carts_user_active_unique
    ON carts (user_id)
    WHERE deleted_at IS NULL;


CREATE TABLE IF NOT EXISTS cart_items
(
    id                 UUID PRIMARY KEY     DEFAULT gen_random_uuid(),

    cart_id            UUID        NOT NULL REFERENCES carts (id) ON DELETE CASCADE,

    product_variant_id UUID        NOT NULL REFERENCES product_variants (id),
    quantity           INT         NOT NULL CHECK (quantity > 0 AND quantity <= 100),

    created_at         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE (cart_id, product_variant_id)
);
CREATE INDEX IF NOT EXISTS idx_cart_items_cart_id ON cart_items (cart_id);
CREATE INDEX IF NOT EXISTS idx_cart_items_variant_id ON cart_items (product_variant_id);


CREATE TABLE IF NOT EXISTS orders
(
    id                              UUID PRIMARY KEY        DEFAULT gen_random_uuid(),

    user_id                         UUID           NOT NULL REFERENCES users (id),

    status                          order_status   NOT NULL DEFAULT 'PENDING',
    total_amount                    NUMERIC(10, 2) NOT NULL CHECK ( total_amount >= 0 ),

    shipping_recipient_first_name   TEXT,
    shipping_recipient_last_name    TEXT,
    shipping_recipient_phone_number TEXT,

    shipping_address                JSONB          NOT NULL,
    shipping_country_id             INT REFERENCES countries (id) ON DELETE RESTRICT,
    shipping_city_id                INT REFERENCES cities (id) ON DELETE RESTRICT,
    shipping_district_id            INT REFERENCES districts (id) ON DELETE RESTRICT,
    shipping_postal_code            TEXT,
    billing_address                 JSONB,

    returned_at                     TIMESTAMPTZ,
    return_reason                   TEXT,
    return_tracking_no              TEXT,

    created_at                      TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                      TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_orders_shipping_city_requires_country
        CHECK (
            shipping_city_id IS NULL
                OR shipping_country_id IS NOT NULL
            )
);
CREATE INDEX IF NOT EXISTS idx_orders_active_status
    ON orders (status)
    WHERE status IN ('PENDING', 'PAID', 'PREPARING', 'SHIPPED');
CREATE INDEX IF NOT EXISTS idx_orders_user_id
    ON orders (user_id);
CREATE INDEX IF NOT EXISTS orders_shipping_country_id_idx
    ON orders (shipping_country_id);
CREATE INDEX IF NOT EXISTS orders_shipping_city_id_idx
    ON orders (shipping_city_id);
CREATE INDEX IF NOT EXISTS orders_shipping_postal_code_idx
    ON orders (shipping_postal_code);
CREATE INDEX IF NOT EXISTS idx_orders_shipping_recipient_phone
    ON orders (shipping_recipient_phone_number);

CREATE TABLE IF NOT EXISTS order_items
(
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    order_id                 UUID           NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    product_variant_id       UUID           NOT NULL REFERENCES product_variants (id),

    quantity                 INT            NOT NULL CHECK (quantity > 0),
    price_at_purchase        NUMERIC(10, 2) NOT NULL CHECK (price_at_purchase >= 0),
    sku_at_purchase          TEXT,
    product_name_at_purchase TEXT,
    variant_snapshot         JSONB
);
CREATE INDEX IF NOT EXISTS idx_order_items_order_id ON order_items (order_id);
CREATE INDEX IF NOT EXISTS idx_order_items_variant_id ON order_items (product_variant_id);

CREATE TABLE IF NOT EXISTS payments
(
    id                     UUID PRIMARY KEY          DEFAULT gen_random_uuid(),

    order_id               UUID             NOT NULL REFERENCES orders (id) ON DELETE CASCADE,

    payment_provider       payment_provider NOT NULL,
    payment_transaction_id TEXT,

    amount                 NUMERIC(10, 2)   NOT NULL CHECK (amount >= 0),
    status                 payment_status   NOT NULL DEFAULT 'PENDING',

    description            TEXT,

    created_at             TIMESTAMPTZ      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMPTZ      NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_one_payment_per_order_provider
    ON payments (order_id, payment_provider);
CREATE UNIQUE INDEX IF NOT EXISTS ux_payments_provider_tx
    ON payments (payment_provider, payment_transaction_id)
    WHERE payment_transaction_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_payments_order_id ON payments (order_id);

CREATE TABLE IF NOT EXISTS refunds
(
    id                 UUID PRIMARY KEY        DEFAULT gen_random_uuid(),
    payment_id         UUID           NOT NULL REFERENCES payments (id) ON DELETE RESTRICT,

    provider_refund_id TEXT,
    amount             NUMERIC(10, 2) NOT NULL CHECK (amount >= 0),
    status             refund_status  NOT NULL DEFAULT 'PENDING',
    reason             TEXT,

    created_at         TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_refunds_payment_id ON refunds (payment_id);

CREATE TABLE refund_items
(
    id            UUID PRIMARY KEY     DEFAULT gen_random_uuid(),

    refund_id     UUID        NOT NULL REFERENCES refunds (id) ON DELETE CASCADE,

    order_item_id UUID REFERENCES order_items (id) ON DELETE RESTRICT,
    quantity      INT,

    amount        NUMERIC(10, 2),

    created_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_refund_items_mode
        CHECK (
            (
                order_item_id IS NOT NULL
                    AND quantity IS NOT NULL
                    AND amount IS NULL
                )
                OR
            (
                order_item_id IS NULL
                    AND quantity IS NULL
                    AND amount IS NOT NULL
                )
            ),

    CONSTRAINT chk_refund_items_quantity_pos
        CHECK (quantity IS NULL OR quantity > 0),
    CONSTRAINT chk_refund_items_amount_pos
        CHECK (amount IS NULL OR amount > 0)
);
CREATE INDEX IF NOT EXISTS idx_refund_items_refund_id
    ON refund_items (refund_id);
CREATE INDEX IF NOT EXISTS idx_refund_items_order_item_id
    ON refund_items (order_item_id)
    WHERE order_item_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS ux_refund_items_refund_order_item
    ON refund_items (refund_id, order_item_id)
    WHERE order_item_id IS NOT NULL;