-- Carts table
CREATE TABLE carts (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP NULL,
    
    CONSTRAINT fk_carts_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_carts_user ON carts(user_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_carts_expires_at ON carts(expires_at) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX idx_carts_user_active ON carts(user_id) WHERE deleted_at IS NULL;

-- Cart items table
CREATE TABLE cart_items (
    id UUID PRIMARY KEY,
    cart_id UUID NOT NULL,
    sku_id UUID NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP NULL,
    
    CONSTRAINT fk_cart_items_cart
        FOREIGN KEY (cart_id) REFERENCES carts(id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_items_sku
        FOREIGN KEY (sku_id) REFERENCES skus(id),
    CONSTRAINT uk_cart_items_cart_sku UNIQUE (cart_id, sku_id) WHERE deleted_at IS NULL
);

CREATE INDEX idx_cart_items_cart ON cart_items(cart_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_cart_items_sku ON cart_items(sku_id) WHERE deleted_at IS NULL;

