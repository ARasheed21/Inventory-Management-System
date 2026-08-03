DELETE FROM order_items;
DELETE FROM orders;
DELETE FROM products;
INSERT INTO products (external_id, name, description, price, currency, quantity_in_stock, version)
VALUES ('SKU-001', 'Benchmark Widget', 'Benchmark product', 12.50, 'USD', 5000, 0);
