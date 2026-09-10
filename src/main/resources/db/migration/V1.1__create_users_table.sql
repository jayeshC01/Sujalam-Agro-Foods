CREATE TABLE users (
    id VARCHAR(255) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    version BIGINT NOT NULL,
    firebase_uid VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255),
    phone_number VARCHAR(255) NOT NULL,
    role VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_firebase_uid UNIQUE (firebase_uid),
    CONSTRAINT uk_users_phone_number UNIQUE (phone_number),
    CONSTRAINT chk_users_role_valid CHECK (role IN ('ADMIN', 'USER')),
    CONSTRAINT chk_users_status_valid CHECK (status IN ('ACTIVE', 'BLOCKED', 'INACTIVE'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_users_email_status ON users (email, status);

CREATE UNIQUE INDEX uk_users_active_email
    ON users ((CASE WHEN status = 'ACTIVE' THEN email END));
