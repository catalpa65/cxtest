INSERT INTO products (id, name, description, price, stock, status, created_at, updated_at)
VALUES
    (1, 'iPhone 16', 'Apple 手机', 6999.00, 100, 'ON_SHELF', NOW(3), NOW(3)),
    (2, 'Sony WH-1000XM6', '降噪耳机', 2899.00, 100, 'ON_SHELF', NOW(3), NOW(3))
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description),
    price = VALUES(price),
    stock = VALUES(stock),
    status = VALUES(status),
    updated_at = NOW(3);
