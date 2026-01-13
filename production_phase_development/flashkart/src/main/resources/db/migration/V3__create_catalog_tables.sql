-- Categories table
CREATE TABLE categories (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500),
    image_url VARCHAR(200),
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP NULL
);

CREATE INDEX idx_categories_active ON categories(active) WHERE deleted_at IS NULL;
CREATE INDEX idx_categories_name ON categories(name) WHERE deleted_at IS NULL;

-- Products table
CREATE TABLE products (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    category_id UUID NOT NULL,
    brand VARCHAR(200),
    image_url VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP NULL,
    
    CONSTRAINT fk_products_category
        FOREIGN KEY (category_id) REFERENCES categories(id)
);

CREATE INDEX idx_products_category ON products(category_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_products_active ON products(active) WHERE deleted_at IS NULL;
CREATE INDEX idx_products_brand ON products(brand) WHERE deleted_at IS NULL;
CREATE INDEX idx_products_name_search ON products USING gin(to_tsvector('english', name)) WHERE deleted_at IS NULL;

-- Prices table (created before SKUs, FK added later to avoid circular dependency)
CREATE TABLE prices (
    id UUID PRIMARY KEY,
    sku_id UUID NULL,
    amount DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    version INTEGER NOT NULL,
    is_current BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP NULL
);

-- SKUs table (created after prices table exists)
CREATE TABLE skus (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL,
    sku_code VARCHAR(50) NOT NULL UNIQUE,
    variant_name VARCHAR(200),
    stock_quantity INTEGER NOT NULL DEFAULT 0,
    reserved_quantity INTEGER NOT NULL DEFAULT 0,
    current_price_id UUID NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP NULL,
    
    CONSTRAINT fk_skus_product
        FOREIGN KEY (product_id) REFERENCES products(id)
);

-- Add foreign keys after both tables are created (resolving circular dependency)
ALTER TABLE prices ADD CONSTRAINT fk_prices_sku
    FOREIGN KEY (sku_id) REFERENCES skus(id) ON DELETE CASCADE;

ALTER TABLE skus ADD CONSTRAINT fk_skus_current_price
    FOREIGN KEY (current_price_id) REFERENCES prices(id);

-- Indexes for prices
CREATE INDEX idx_prices_sku ON prices(sku_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_prices_current ON prices(sku_id, is_current) WHERE deleted_at IS NULL AND is_current = true;
CREATE INDEX idx_prices_version ON prices(sku_id, version) WHERE deleted_at IS NULL;

-- Indexes for skus
CREATE INDEX idx_skus_product ON skus(product_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_skus_code ON skus(sku_code) WHERE deleted_at IS NULL;
CREATE INDEX idx_skus_active ON skus(active) WHERE deleted_at IS NULL;
CREATE INDEX idx_skus_stock ON skus(stock_quantity) WHERE deleted_at IS NULL AND active = true;
