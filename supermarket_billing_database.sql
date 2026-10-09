-- ============================================================
-- Supermarket Billing Project - MySQL database script
-- Database: supermarket_billing
--
-- Includes:
--   1. Tables and relationships
--   2. JOIN query for sales/customer/product details
--   3. Nested subquery for products sold above average quantity
--   4. Stored procedure: sp_create_sale
--   5. Stored function: calculate_invoice_total
--   6. Trigger: trg_reduce_inventory
--
-- IMPORTANT:
-- This script is based on the column names visible in the Java code
-- shared in the conversation. Compare it with your existing database
-- and Entity classes before running it on a database containing data.
-- CREATE TABLE IF NOT EXISTS does not alter an existing table's schema.
-- The procedure/function/trigger definitions below are dropped and
-- recreated when this script is run.
-- ============================================================

CREATE DATABASE IF NOT EXISTS supermarket_billing;
USE supermarket_billing;

-- ============================================================
-- 1. TABLE CREATION
-- ============================================================

CREATE TABLE IF NOT EXISTS customers (
    customer_id INT NOT NULL AUTO_INCREMENT,
    customer_name VARCHAR(150) NOT NULL,
    PRIMARY KEY (customer_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS products (
    product_id INT NOT NULL AUTO_INCREMENT,
    product_name VARCHAR(150) NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    quantity INT NOT NULL DEFAULT 0,
    category VARCHAR(100),
    PRIMARY KEY (product_id),
    CONSTRAINT chk_products_price CHECK (price >= 0),
    CONSTRAINT chk_products_quantity CHECK (quantity >= 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS sales (
    sale_id INT NOT NULL AUTO_INCREMENT,
    customer_id INT NOT NULL,
    sale_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (sale_id),
    INDEX idx_sales_customer_id (customer_id),
    CONSTRAINT fk_sales_customer
        FOREIGN KEY (customer_id) REFERENCES customers(customer_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS sale_items (
    sale_item_id INT NOT NULL AUTO_INCREMENT,
    sale_id INT NOT NULL,
    product_id INT NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    PRIMARY KEY (sale_item_id),
    INDEX idx_sale_items_sale_id (sale_id),
    INDEX idx_sale_items_product_id (product_id),
    CONSTRAINT chk_sale_items_quantity CHECK (quantity > 0),
    CONSTRAINT chk_sale_items_unit_price CHECK (unit_price >= 0),
    CONSTRAINT fk_sale_items_sale
        FOREIGN KEY (sale_id) REFERENCES sales(sale_id),
    CONSTRAINT fk_sale_items_product
        FOREIGN KEY (product_id) REFERENCES products(product_id)
) ENGINE=InnoDB;

-- ============================================================
-- 2. JOIN QUERY: sales with customer and product information
-- Matches SaleRepository.getSalesWithCustomerAndProduct().
-- ============================================================

SELECT
    s.sale_id AS saleId,
    s.sale_date AS saleDate,
    c.customer_id AS customerId,
    c.customer_name AS customerName,
    p.product_id AS productId,
    p.product_name AS productName,
    si.quantity AS quantity,
    si.unit_price AS unitPrice,
    (si.quantity * si.unit_price) AS itemTotal
FROM sales s
JOIN customers c ON s.customer_id = c.customer_id
JOIN sale_items si ON s.sale_id = si.sale_id
JOIN products p ON si.product_id = p.product_id
ORDER BY s.sale_date DESC;

-- ============================================================
-- 3. NESTED SUBQUERY: products whose total sold quantity is above
-- the average product-wise total quantity sold.
-- Matches the query shared for ProductRepository.
-- ============================================================

SELECT p.*
FROM products p
JOIN sale_items si ON p.product_id = si.product_id
GROUP BY p.product_id, p.product_name, p.price, p.quantity, p.category
HAVING SUM(si.quantity) > (
    SELECT AVG(product_quantity)
    FROM (
        SELECT SUM(quantity) AS product_quantity
        FROM sale_items
        GROUP BY product_id
    ) AS average_sales
);

-- ============================================================
-- 4. TRIGGER: reduce product inventory after a sale item is inserted
-- Event: AFTER INSERT on sale_items.
-- Uses products.quantity as the stock column shown in SaleService.
-- ============================================================

DROP TRIGGER IF EXISTS trg_reduce_inventory;

DELIMITER $$
CREATE TRIGGER trg_reduce_inventory
AFTER INSERT ON sale_items
FOR EACH ROW
BEGIN
    UPDATE products
    SET quantity = quantity - NEW.quantity
    WHERE product_id = NEW.product_id;
END$$
DELIMITER ;

-- ============================================================
-- 5. FUNCTION: calculate invoice total for one sale
-- Total = SUM(quantity * unit_price) for the supplied sale ID.
-- ============================================================

DROP FUNCTION IF EXISTS calculate_invoice_total;

DELIMITER $$
CREATE FUNCTION calculate_invoice_total(p_sale_id INT)
RETURNS DECIMAL(12,2)
DETERMINISTIC
READS SQL DATA
BEGIN
    DECLARE v_total DECIMAL(12,2) DEFAULT 0.00;

    SELECT COALESCE(SUM(quantity * unit_price), 0.00)
      INTO v_total
    FROM sale_items
    WHERE sale_id = p_sale_id;

    RETURN v_total;
END$$
DELIMITER ;

-- ============================================================
-- 6. STORED PROCEDURE: create a single-product sale
-- Inputs: customer ID, product ID, quantity.
-- Returns the new sale ID as a result set with column name saleId.
-- Trigger trg_reduce_inventory decreases stock when sale_items is inserted.
-- ============================================================

DROP PROCEDURE IF EXISTS sp_create_sale;

DELIMITER $$
CREATE PROCEDURE sp_create_sale(
    IN p_customer_id INT,
    IN p_product_id INT,
    IN p_quantity INT
)
BEGIN
    DECLARE v_customer_count INT DEFAULT 0;
    DECLARE v_stock INT DEFAULT NULL;
    DECLARE v_price DECIMAL(10,2) DEFAULT NULL;
    DECLARE v_sale_id INT DEFAULT NULL;

    IF p_customer_id IS NULL OR p_customer_id <= 0
       OR p_product_id IS NULL OR p_product_id <= 0
       OR p_quantity IS NULL OR p_quantity <= 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Customer ID, product ID and quantity must be positive values';
    END IF;

    SELECT COUNT(*)
      INTO v_customer_count
    FROM customers
    WHERE customer_id = p_customer_id;

    IF v_customer_count = 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Customer not found';
    END IF;

    SELECT quantity, price
      INTO v_stock, v_price
    FROM products
    WHERE product_id = p_product_id;

    IF v_stock IS NULL OR v_price IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Product not found';
    END IF;

    IF v_stock < p_quantity THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Insufficient stock';
    END IF;

    INSERT INTO sales (customer_id, sale_date)
    VALUES (p_customer_id, CURRENT_TIMESTAMP);

    SET v_sale_id = LAST_INSERT_ID();

    INSERT INTO sale_items (sale_id, product_id, quantity, unit_price)
    VALUES (v_sale_id, p_product_id, p_quantity, v_price);

    SELECT v_sale_id AS saleId;
END$$
DELIMITER ;

-- ============================================================
-- Useful verification commands (run separately when needed)
-- ============================================================
-- SHOW TABLES;
-- SHOW TRIGGERS FROM supermarket_billing;
-- SHOW CREATE TRIGGER trg_reduce_inventory;
-- SHOW CREATE PROCEDURE sp_create_sale;
-- SHOW CREATE FUNCTION calculate_invoice_total;
-- CALL sp_create_sale(1, 1, 2);
-- SELECT calculate_invoice_total(1);
