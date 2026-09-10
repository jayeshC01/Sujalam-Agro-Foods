CREATE TABLE products (
    id VARCHAR(255) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    version BIGINT NOT NULL,
    category VARCHAR(32) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(2000),
    image_url VARCHAR(500) NOT NULL,
    health_benefits VARCHAR(2000),
    price DECIMAL(10, 2) NOT NULL,
    quantity INT NOT NULL,
    gst_rate DECIMAL(5, 4) NOT NULL,
    status VARCHAR(32) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_products_name UNIQUE (name),
    CONSTRAINT chk_products_category_valid CHECK (category IN ('EDIBLE', 'NOT_EDIBLE')),
    CONSTRAINT chk_products_status_valid CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT chk_products_name_not_blank CHECK (TRIM(name) <> ''),
    CONSTRAINT chk_products_image_url_not_blank CHECK (TRIM(image_url) <> ''),
    CONSTRAINT chk_products_price_non_negative CHECK (price >= 0),
    CONSTRAINT chk_products_quantity_non_negative CHECK (quantity >= 0),
    CONSTRAINT chk_products_gst_rate_valid CHECK (gst_rate >= 0 AND gst_rate <= 1.0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_products_status_category ON products (status, category);
CREATE INDEX idx_products_category ON products (category);
