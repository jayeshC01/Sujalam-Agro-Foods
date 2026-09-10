CREATE TABLE orders (
    id VARCHAR(255) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    version BIGINT NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    order_status VARCHAR(32) NOT NULL,
    payment_method VARCHAR(32) NOT NULL,
    payment_status VARCHAR(32) NOT NULL,
    customer_id VARCHAR(255) NOT NULL,
    sub_total DECIMAL(12, 2),
    tax_amount DECIMAL(12, 2),
    delivery_charge DECIMAL(12, 2),
    grand_total DECIMAL(12, 2) NOT NULL,
    shipping_recipient_name VARCHAR(255) NOT NULL,
    shipping_phone_number VARCHAR(255) NOT NULL,
    shipping_address_line1 VARCHAR(255) NOT NULL,
    shipping_address_line2 VARCHAR(255),
    shipping_city VARCHAR(255) NOT NULL,
    shipping_state VARCHAR(255) NOT NULL,
    shipping_postal_code VARCHAR(255) NOT NULL,
    shipping_country VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_orders_user_idempotency UNIQUE (customer_id, idempotency_key),
    CONSTRAINT fk_orders_customer FOREIGN KEY (customer_id) REFERENCES users (id),
    CONSTRAINT chk_orders_status_valid CHECK (order_status IN ('PROCESSING', 'PACKED', 'SHIPPED', 'COMPLETED', 'CANCELED')),
    CONSTRAINT chk_orders_payment_method_valid CHECK (payment_method IN ('COD', 'UPI', 'CARD')),
    CONSTRAINT chk_orders_payment_status_valid CHECK (payment_status IN ('PENDING', 'PAID', 'FAILED')),
    CONSTRAINT chk_orders_grand_total_non_negative CHECK (grand_total >= 0.0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_orders_idempotency_key ON orders (idempotency_key);
CREATE INDEX idx_orders_status ON orders (order_status);
CREATE INDEX idx_orders_created_at ON orders (created_at);
