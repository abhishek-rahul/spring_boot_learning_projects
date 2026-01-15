-- Orders table
CREATE TABLE orders (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    order_number VARCHAR(50) NOT NULL UNIQUE,
    status VARCHAR(50) NOT NULL,
    total_amount DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    cart_id UUID NULL, -- Reference to the cart that was checked out
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP NULL,
    
    CONSTRAINT fk_orders_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_orders_cart
        FOREIGN KEY (cart_id) REFERENCES carts(id) ON DELETE SET NULL,
    CONSTRAINT chk_orders_status
        CHECK (status IN ('DRAFT', 'PENDING_PAYMENT', 'PAYMENT_FAILED', 'PAID', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED'))
);

CREATE INDEX idx_orders_user ON orders(user_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_orders_status ON orders(status) WHERE deleted_at IS NULL;
CREATE INDEX idx_orders_order_number ON orders(order_number) WHERE deleted_at IS NULL;
CREATE INDEX idx_orders_created_at ON orders(created_at DESC) WHERE deleted_at IS NULL;

-- Order items table
CREATE TABLE order_items (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    sku_id UUID NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    unit_price DECIMAL(10, 2) NOT NULL,
    total_price DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP NULL,
    
    CONSTRAINT fk_order_items_order
        FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_order_items_sku
        FOREIGN KEY (sku_id) REFERENCES skus(id)
);

CREATE INDEX idx_order_items_order ON order_items(order_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_order_items_sku ON order_items(sku_id) WHERE deleted_at IS NULL;

-- Payment intents table
CREATE TABLE payment_intents (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    status VARCHAR(50) NOT NULL,
    payment_provider VARCHAR(50) NULL, -- e.g., 'STRIPE', 'PAYPAL'
    provider_payment_id VARCHAR(255) NULL, -- External payment gateway ID
    failure_reason TEXT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP NULL,
    
    CONSTRAINT fk_payment_intents_order
        FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT chk_payment_intents_status
        CHECK (status IN ('CREATED', 'PENDING', 'SUCCEEDED', 'FAILED', 'CANCELLED'))
);

CREATE INDEX idx_payment_intents_order ON payment_intents(order_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_payment_intents_status ON payment_intents(status) WHERE deleted_at IS NULL;
CREATE INDEX idx_payment_intents_provider_id ON payment_intents(provider_payment_id) WHERE deleted_at IS NULL AND provider_payment_id IS NOT NULL;

-- Idempotency keys table
-- This table ensures that checkout requests with the same idempotency key are processed only once
CREATE TABLE idempotency_keys (
    id UUID PRIMARY KEY,
    key_hash VARCHAR(64) NOT NULL UNIQUE, -- SHA-256 hash of the idempotency key
    user_id UUID NOT NULL,
    request_path VARCHAR(255) NOT NULL,
    response_status_code INTEGER NULL,
    response_body TEXT NULL, -- JSON response body for successful requests
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    
    CONSTRAINT fk_idempotency_keys_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_idempotency_keys_hash ON idempotency_keys(key_hash);
CREATE INDEX idx_idempotency_keys_user ON idempotency_keys(user_id);
CREATE INDEX idx_idempotency_keys_expires_at ON idempotency_keys(expires_at);

-- Inventory reservations table (for tracking reserved stock during checkout)
CREATE TABLE inventory_reservations (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    sku_id UUID NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    status VARCHAR(50) NOT NULL,
    expires_at TIMESTAMP NOT NULL, -- Reservation expires if not confirmed
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP NULL,
    
    CONSTRAINT fk_inventory_reservations_order
        FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_inventory_reservations_sku
        FOREIGN KEY (sku_id) REFERENCES skus(id),
    CONSTRAINT chk_inventory_reservations_status
        CHECK (status IN ('RESERVED', 'CONFIRMED', 'RELEASED', 'EXPIRED'))
);

CREATE INDEX idx_inventory_reservations_order ON inventory_reservations(order_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_inventory_reservations_sku ON inventory_reservations(sku_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_inventory_reservations_status ON inventory_reservations(status) WHERE deleted_at IS NULL;
CREATE INDEX idx_inventory_reservations_expires_at ON inventory_reservations(expires_at) WHERE deleted_at IS NULL AND status = 'RESERVED';
