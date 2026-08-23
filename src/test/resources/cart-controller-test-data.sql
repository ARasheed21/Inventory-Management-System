DELETE FROM cart_items;
DELETE FROM products;
INSERT INTO products (external_id, name, description, price, currency, quantity_in_stock, version)
VALUES ('SKU-001', 'Widget', 'Shopping product', 12.50, 'USD', 25, 0);
