CREATE TABLE order_details (
    id VARCHAR(255) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    version BIGINT NOT NULL,
    order_id VARCHAR(255) NOT NULL,
    product_id VARCHAR(255) NOT NULL,
    ordered_qty INT NOT NULL,
    unit_price DECIMAL(10, 2) NOT NULL,
    sub_total DECIMAL(12, 2) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_order_details_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_details_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT chk_order_details_qty_valid CHECK (ordered_qty >= 1),
    CONSTRAINT chk_order_details_unit_price_valid CHECK (unit_price >= 0.0),
    CONSTRAINT chk_order_details_sub_total_valid CHECK (sub_total >= 0.0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
