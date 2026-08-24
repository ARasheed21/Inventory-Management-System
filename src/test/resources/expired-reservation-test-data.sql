DELETE FROM order_items;
DELETE FROM orders;
DELETE FROM products;
INSERT INTO products (external_id, name, description, price, currency, quantity_in_stock, version)
VALUES ('SKU-001', 'Widget', 'Test product', 12.50, 'USD', 25, 0);
INSERT INTO orders (external_id, customer_id, status, total_amount, currency, created_at, updated_at, reserved_until, version)
VALUES ('ord-expired-1', 'customer', 'PENDING', 12.50, 'USD',
    DATEADD('MINUTE', -20, CURRENT_TIMESTAMP),
    DATEADD('MINUTE', -20, CURRENT_TIMESTAMP),
    DATEADD('MINUTE', -5, CURRENT_TIMESTAMP), 0);
INSERT INTO order_items (order_id, product_id, quantity, unit_price, sku)
SELECT id, 'SKU-001', 1, 12.50, 'SKU-001' FROM orders WHERE external_id = 'ord-expired-1';
