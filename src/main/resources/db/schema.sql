CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(128) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(32) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    UNIQUE KEY uk_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS products (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(64) NOT NULL,
    description VARCHAR(512),
    price DECIMAL(12, 2) NOT NULL,
    stock INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    KEY idx_products_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    total_amount DECIMAL(12, 2) NOT NULL,
    expires_at DATETIME(3),
    payment_transaction_id VARCHAR(128),
    paid_at DATETIME(3),
    completed_at DATETIME(3),
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    KEY idx_orders_user_id (user_id),
    KEY idx_orders_status (status),
    KEY idx_orders_payment_tx (payment_transaction_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS order_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(64) NOT NULL,
    unit_price DECIMAL(12, 2) NOT NULL,
    quantity INT NOT NULL,
    line_amount DECIMAL(12, 2) NOT NULL,
    KEY idx_order_items_order_id (order_id),
    CONSTRAINT fk_order_items_order_id FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS mq_process_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_id VARCHAR(64) NOT NULL,
    type VARCHAR(32) NOT NULL,
    order_id BIGINT,
    transaction_id VARCHAR(128),
    attempts INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    message VARCHAR(1024),
    processed_at DATETIME(3) NOT NULL,
    KEY idx_mq_process_logs_processed_at (processed_at),
    KEY idx_mq_process_logs_event_id (event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS mq_dead_letters (
    event_id VARCHAR(64) PRIMARY KEY,
    type VARCHAR(32) NOT NULL,
    order_id BIGINT,
    transaction_id VARCHAR(128),
    attempts INT NOT NULL,
    last_error VARCHAR(1024),
    failed_at DATETIME(3) NOT NULL,
    KEY idx_mq_dead_letters_failed_at (failed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
