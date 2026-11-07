-- ============================================================
-- Inventory Management System — Schema (DDL Only)
-- v2: supersedes the DDL section of inventory_db.sql
-- Run this file first, then run inventory_db_data.sql.
-- ============================================================

SET FOREIGN_KEY_CHECKS = 0;

-- CREATE DATABASE IF NOT EXISTS inventory_db;
-- USE inventory_db;

-- ------------------------------------------------------------
-- DROP all tables in reverse FK dependency order
-- ------------------------------------------------------------
DROP TABLE IF EXISTS audit_logs;
DROP TABLE IF EXISTS proposals;
DROP TABLE IF EXISTS product_suppliers;
DROP TABLE IF EXISTS transactions;
DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS warehouses;
DROP TABLE IF EXISTS suppliers;
DROP TABLE IF EXISTS categories;
DROP TABLE IF EXISTS users;

-- ============================================================
-- CREATE TABLES (FK dependency order)
-- ============================================================

-- Users
CREATE TABLE users (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  created_at   DATETIME     DEFAULT NULL,
  updated_at   DATETIME     DEFAULT NULL,
  email        VARCHAR(255) NOT NULL,
  name         VARCHAR(255) NOT NULL,
  password     VARCHAR(255) NOT NULL,
  phone_number VARCHAR(255) NOT NULL,
  role         ENUM('ADMIN','MANAGER','STAFF') DEFAULT NULL,
  is_active    BOOLEAN      NOT NULL DEFAULT TRUE,
  created_by   BIGINT       DEFAULT NULL,
  updated_by   BIGINT       DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY (email),
  FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL ON UPDATE CASCADE,
  FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL ON UPDATE CASCADE
);

-- Categories
CREATE TABLE categories (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  name          VARCHAR(255) NOT NULL,
  description   VARCHAR(500) DEFAULT NULL,
  image_url     VARCHAR(255) DEFAULT NULL,
  is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
  display_order INT          NOT NULL DEFAULT 0,
  created_at    DATETIME     DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by    BIGINT       DEFAULT NULL,
  updated_by    BIGINT       DEFAULT NULL,
  PRIMARY KEY (id),
  FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL ON UPDATE CASCADE,
  FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL ON UPDATE CASCADE
);

-- Warehouses
CREATE TABLE warehouses (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  name         VARCHAR(255) NOT NULL,
  location     VARCHAR(255) NOT NULL,
  address      VARCHAR(500) DEFAULT NULL,
  capacity     INT          DEFAULT NULL,
  manager_name VARCHAR(255) DEFAULT NULL,
  is_active    BOOLEAN      NOT NULL DEFAULT TRUE,
  created_at   DATETIME     DEFAULT CURRENT_TIMESTAMP,
  updated_at   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by   BIGINT       DEFAULT NULL,
  updated_by   BIGINT       DEFAULT NULL,
  PRIMARY KEY (id),
  FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL ON UPDATE CASCADE,
  FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL ON UPDATE CASCADE
);

-- Suppliers
CREATE TABLE suppliers (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  name         VARCHAR(255) NOT NULL,
  address      VARCHAR(255) DEFAULT NULL,
  contact_info VARCHAR(255) NOT NULL,
  email        VARCHAR(255) DEFAULT NULL,
  website      VARCHAR(255) DEFAULT NULL,
  tax_code     VARCHAR(50)  DEFAULT NULL,
  status       ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
  created_at   DATETIME     DEFAULT CURRENT_TIMESTAMP,
  updated_at   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by   BIGINT       DEFAULT NULL,
  updated_by   BIGINT       DEFAULT NULL,
  PRIMARY KEY (id),
  FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL ON UPDATE CASCADE,
  FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL ON UPDATE CASCADE
);

-- Products
CREATE TABLE products (
  id                BIGINT         NOT NULL AUTO_INCREMENT,
  created_at        DATETIME       DEFAULT NULL,
  updated_at        DATETIME       DEFAULT NULL,
  description       VARCHAR(255)   DEFAULT NULL,
  expiry_date       DATETIME       DEFAULT NULL,
  image_url         VARCHAR(255)   DEFAULT NULL,
  name              VARCHAR(255)   NOT NULL,
  price             DECIMAL(38,2)  DEFAULT NULL,
  sku               VARCHAR(255)   NOT NULL,
  stock_quantity    INT            DEFAULT NULL,
  reorder_threshold INT            NOT NULL DEFAULT 10,
  category_id       BIGINT         DEFAULT NULL,
  warehouse_id      BIGINT         DEFAULT NULL,
  is_deleted        BOOLEAN        NOT NULL DEFAULT FALSE,
  created_by        BIGINT         DEFAULT NULL,
  updated_by        BIGINT         DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY (sku),
  KEY (category_id),
  KEY (warehouse_id),
  KEY (created_by),
  KEY (updated_by),
  FOREIGN KEY (category_id)  REFERENCES categories(id)  ON DELETE CASCADE  ON UPDATE CASCADE,
  FOREIGN KEY (warehouse_id) REFERENCES warehouses(id)  ON DELETE SET NULL ON UPDATE CASCADE,
  FOREIGN KEY (created_by)   REFERENCES users(id)       ON DELETE SET NULL ON UPDATE CASCADE,
  FOREIGN KEY (updated_by)   REFERENCES users(id)       ON DELETE SET NULL ON UPDATE CASCADE,
  CHECK (stock_quantity >= 0)
);

-- Product–Supplier junction table
CREATE TABLE product_suppliers (
  product_id   BIGINT        NOT NULL,
  supplier_id  BIGINT        NOT NULL,
  unit_cost    DECIMAL(38,2) DEFAULT NULL,
  is_preferred BOOLEAN       NOT NULL DEFAULT FALSE,
  created_at   DATETIME      DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (product_id, supplier_id),
  FOREIGN KEY (product_id)  REFERENCES products(id)  ON DELETE CASCADE ON UPDATE CASCADE,
  FOREIGN KEY (supplier_id) REFERENCES suppliers(id) ON DELETE CASCADE ON UPDATE CASCADE
);

-- Warehouse–Supplier junction table
CREATE TABLE warehouse_suppliers (
  warehouse_id BIGINT        NOT NULL,
  supplier_id  BIGINT        NOT NULL,
  is_preferred BOOLEAN       NOT NULL DEFAULT FALSE,
  created_at   DATETIME      DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (warehouse_id, supplier_id),
  FOREIGN KEY (warehouse_id) REFERENCES warehouses(id) ON DELETE CASCADE ON UPDATE CASCADE,
  FOREIGN KEY (supplier_id)  REFERENCES suppliers(id)  ON DELETE CASCADE ON UPDATE CASCADE
);

-- Product–Warehouse junction table (Inventory levels)
CREATE TABLE product_warehouses (
  product_id   BIGINT        NOT NULL,
  warehouse_id BIGINT        NOT NULL,
  quantity     INT           NOT NULL DEFAULT 0,
  created_at   DATETIME      DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (product_id, warehouse_id),
  FOREIGN KEY (product_id)   REFERENCES products(id)  ON DELETE CASCADE ON UPDATE CASCADE,
  FOREIGN KEY (warehouse_id) REFERENCES warehouses(id) ON DELETE CASCADE ON UPDATE CASCADE,
  CHECK (quantity >= 0)
);

-- Transactions
CREATE TABLE transactions (
  id               BIGINT        NOT NULL AUTO_INCREMENT,
  created_at       DATETIME      DEFAULT NULL,
  description      VARCHAR(255)  DEFAULT NULL,
  note             VARCHAR(255)  DEFAULT NULL,
  status           ENUM('CANCELLED','COMPLETED','PENDING','PROCESSING') DEFAULT NULL,
  total_price      DECIMAL(38,2) DEFAULT NULL,
  total_products   INT           DEFAULT NULL,
  transaction_type ENUM('PURCHASE','RETURN_TO_SUPPLIER','SALE') DEFAULT NULL,
  updated_at       DATETIME      DEFAULT NULL,
  product_id       BIGINT        DEFAULT NULL,
  supplier_id      BIGINT        DEFAULT NULL,
  user_id          BIGINT        DEFAULT NULL,
  created_by       BIGINT        DEFAULT NULL,
  updated_by       BIGINT        DEFAULT NULL,
  PRIMARY KEY (id),
  FOREIGN KEY (product_id)  REFERENCES products(id)  ON DELETE SET NULL ON UPDATE CASCADE,
  FOREIGN KEY (supplier_id) REFERENCES suppliers(id) ON DELETE SET NULL ON UPDATE CASCADE,
  FOREIGN KEY (user_id)     REFERENCES users(id)     ON DELETE SET NULL ON UPDATE CASCADE,
  FOREIGN KEY (created_by)  REFERENCES users(id)     ON DELETE SET NULL ON UPDATE CASCADE,
  FOREIGN KEY (updated_by)  REFERENCES users(id)     ON DELETE SET NULL ON UPDATE CASCADE
);

-- Notifications
CREATE TABLE notifications (
  id          BIGINT        NOT NULL AUTO_INCREMENT,
  user_id     BIGINT        NOT NULL,
  title       VARCHAR(255)  NOT NULL,
  message     TEXT          NOT NULL,
  type        ENUM('INFO','WARNING','ERROR','SUCCESS') DEFAULT 'INFO',
  event_type  VARCHAR(50)   DEFAULT NULL,
  entity_type VARCHAR(50)   DEFAULT NULL,
  entity_id   BIGINT        DEFAULT NULL,
  action_url  VARCHAR(255)  DEFAULT NULL,
  dedupe_key  VARCHAR(255)  DEFAULT NULL,
  is_read     BOOLEAN       NOT NULL DEFAULT FALSE,
  created_at  DATETIME      DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE ON UPDATE CASCADE,
  INDEX (user_id),
  INDEX (is_read)
);

-- ============================================================
-- INDEXES (for query performance)
-- ============================================================

-- Products: filter by is_deleted (soft delete), search by name/SKU/description
CREATE INDEX idx_products_is_deleted          ON products(is_deleted);
CREATE INDEX idx_products_name_sku_description ON products(name, sku, description);

-- Transactions: filter by status, type, date range, sort by created_at
CREATE INDEX idx_transactions_status     ON transactions(status);
CREATE INDEX idx_transactions_type       ON transactions(transaction_type);
CREATE INDEX idx_transactions_created_at ON transactions(created_at);
CREATE INDEX idx_transactions_product_id ON transactions(product_id);

-- Suppliers: filter by status
CREATE INDEX idx_suppliers_status ON suppliers(status);

-- Missing FK indexes (DB-1)
CREATE INDEX idx_product_suppliers_supplier_id ON product_suppliers(supplier_id);
CREATE INDEX idx_transactions_supplier_id       ON transactions(supplier_id);
CREATE INDEX idx_transactions_user_id           ON transactions(user_id);

-- Warehouse–Supplier junction: FK index on supplier_id side
CREATE INDEX idx_warehouse_suppliers_supplier_id ON warehouse_suppliers(supplier_id);

-- Product–Warehouse junction: FK index on warehouse_id side
CREATE INDEX idx_product_warehouses_warehouse_id ON product_warehouses(warehouse_id);

-- Categories: sort by display_order
CREATE INDEX idx_categories_display_order ON categories(display_order);

-- Notifications: optimized indexes for query patterns
-- Unread count: user_id + is_read (covers countUnread)
CREATE INDEX idx_notifications_user_read ON notifications(user_id, is_read);
-- Dedupe check: user_id + dedupe_key + is_read + created_at (covers findUnreadByDedupeKey with sort)
CREATE INDEX idx_notifications_dedupe ON notifications(user_id, dedupe_key, is_read, created_at DESC);
-- Paginated fetch with type filter: user_id + type + created_at (covers findByUserId)
CREATE INDEX idx_notifications_user_type_created ON notifications(user_id, type, created_at DESC);
-- Paginated fetch without type filter: user_id + created_at (covers findByUserId)
CREATE INDEX idx_notifications_user_created ON notifications(user_id, created_at DESC);

-- ------------------------------------------------------------
-- Proposals
-- ------------------------------------------------------------
CREATE TABLE proposals (
  id               BIGINT        NOT NULL AUTO_INCREMENT,
  transaction_type ENUM('PURCHASE','RETURN_TO_SUPPLIER','SALE') NOT NULL,
  status           ENUM('PENDING','APPROVED','REJECTED') DEFAULT 'PENDING',
  product_id       BIGINT        DEFAULT NULL,
  supplier_id      BIGINT        DEFAULT NULL,
  quantity         INT           NOT NULL,
  description      VARCHAR(255)  DEFAULT NULL,
  total_price      DECIMAL(38,2) DEFAULT NULL,
  proposed_by      BIGINT        NOT NULL,
  reviewed_by      BIGINT        DEFAULT NULL,
  review_note      VARCHAR(500)  DEFAULT NULL,
  created_at       DATETIME      DEFAULT CURRENT_TIMESTAMP,
  reviewed_at      DATETIME      DEFAULT NULL,
  transaction_id   BIGINT        DEFAULT NULL,
  PRIMARY KEY (id),
  FOREIGN KEY (proposed_by)    REFERENCES users(id)        ON DELETE RESTRICT,
  FOREIGN KEY (reviewed_by)    REFERENCES users(id)        ON DELETE SET NULL,
  FOREIGN KEY (transaction_id) REFERENCES transactions(id) ON DELETE SET NULL,
  FOREIGN KEY (product_id)     REFERENCES products(id)     ON DELETE SET NULL,
  FOREIGN KEY (supplier_id)    REFERENCES suppliers(id)    ON DELETE SET NULL
);

-- ------------------------------------------------------------
-- Audit Logs
-- ------------------------------------------------------------
CREATE TABLE audit_logs (
  id          BIGINT        NOT NULL AUTO_INCREMENT,
  user_id     BIGINT        NOT NULL,
  action      VARCHAR(100)  NOT NULL,
  entity_type VARCHAR(50)   NOT NULL,
  entity_id   BIGINT        NOT NULL,
  details     TEXT          DEFAULT NULL,
  created_at  DATETIME      DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT
);

-- Proposals indexes
CREATE INDEX idx_proposals_status      ON proposals(status);
CREATE INDEX idx_proposals_proposed_by ON proposals(proposed_by);
CREATE INDEX idx_proposals_product_id  ON proposals(product_id);

-- Audit Logs indexes
CREATE INDEX idx_audit_logs_user_id    ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_entity     ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);

-- ============================================================
-- SEED DATA FOR NEW INVENTORY SCHEMA
-- ============================================================

SET FOREIGN_KEY_CHECKS = 0;

-- ------------------------------------------------------------
-- Users
-- ------------------------------------------------------------
INSERT INTO users (id, created_at, updated_at, email, name, password, phone_number, role, is_active, created_by, updated_by) VALUES
(1, '2025-11-21 16:15:10', '2025-11-21 16:15:10', 'admin@gmail.com',   'admin user',   '$2a$10$eUSKrqVAFm/rThmuBv858Oc/5pemWwpnm3PufrkaFUj.YLV6xxhtq', '+84363636406', 'ADMIN', TRUE, NULL, NULL),
(2, '2025-11-21 16:15:32', '2025-11-21 16:15:32', 'manager@gmail.com', 'manager user', '$2a$10$WMNX5Tj5zz6T.qDQJqbd2O28PEHZA8t0BprwLnofZz01nJW3/EgwO', '+84363636406', 'MANAGER', TRUE, 1, 1),
(3, '2025-11-21 16:15:50', '2025-11-21 16:15:50', 'staff@gmail.com',   'staff user',   '$2a$10$WMNX5Tj5zz6T.qDQJqbd2O28PEHZA8t0BprwLnofZz01nJW3/EgwO', '+84363636407', 'STAFF', TRUE, 1, 1);

-- ------------------------------------------------------------
-- Categories
-- ------------------------------------------------------------
INSERT INTO categories (id, name, description, image_url, is_active, display_order, created_at) VALUES
(1, 'Electronics',  'Consumer electronics, gadgets and tech accessories',       NULL, TRUE, 1, '2024-01-01 00:00:00'),
(2, 'Clothing',     'Apparel, footwear and fashion items',                       NULL, TRUE, 2, '2024-01-01 00:00:00'),
(3, 'Kitchen',      'Kitchen appliances, cookware and dining accessories',       NULL, TRUE, 3, '2024-01-01 00:00:00'),
(4, 'Accessories',  'Fashion accessories, jewelry and personal items',           NULL, TRUE, 4, '2024-01-01 00:00:00'),
(5, 'Toys',         'Children toys, games and educational products',             NULL, TRUE, 5, '2024-01-01 00:00:00'),
(6, 'Sports',       'Sports equipment, fitness gear and outdoor products',       NULL, TRUE, 6, '2024-01-01 00:00:00'),
(7, 'Home Decor',   'Home decoration, furnishings and lifestyle accessories',    NULL, TRUE, 7, '2024-01-01 00:00:00');

-- ------------------------------------------------------------
-- Warehouses
-- ------------------------------------------------------------
INSERT INTO warehouses (id, name, location, address, capacity, manager_name, is_active, created_at) VALUES
(1, 'Main Warehouse',    'Ho Chi Minh City', '123 Industrial Zone, District 12, HCMC',          5000, 'Nguyen Van An',   TRUE, '2023-06-01 00:00:00'),
(2, 'North Depot',       'Hanoi',            '45 Noi Bai Industrial Park, Hanoi',               3000, 'Tran Thi Bich',   TRUE, '2023-06-01 00:00:00'),
(3, 'Central Storage',   'Da Nang',          '89 Hoa Khanh Industrial Zone, Da Nang',           2000, 'Le Minh Tuan',    TRUE, '2023-09-01 00:00:00'),
(4, 'East Wing',         'Ho Chi Minh City', '456 Binh Duong Avenue, District 9, HCMC',         2500, 'Pham Thi Thu',    TRUE, '2024-01-01 00:00:00'),
(5, 'Overflow Depot',    'Ho Chi Minh City', '789 Long An Road, Binh Chanh, HCMC',              4000, 'Vo Van Duc',      TRUE, '2024-03-01 00:00:00');

-- ------------------------------------------------------------
-- Suppliers
-- ------------------------------------------------------------
INSERT INTO suppliers (id, name, address, contact_info, email, website, tax_code, status, created_at, updated_at) VALUES
(1,  'Alpha Supply Co',        'Ho Chi Minh City', '0901000001', 'contact@alphasupply.vn',    'www.alphasupply.vn',    'TAX0000001', 'ACTIVE', '2023-01-10 00:00:00', '2025-01-10 00:00:00'),
(2,  'Bravo Traders',          'Hanoi',            '0901000002', 'info@bravotraders.vn',       'www.bravotraders.vn',   'TAX0000002', 'ACTIVE', '2023-01-15 00:00:00', '2025-01-15 00:00:00'),
(3,  'Charlie Imports',        'Da Nang',          '0901000003', 'hello@charlieimports.vn',   'www.charlieimports.vn', 'TAX0000003', 'ACTIVE', '2023-02-01 00:00:00', '2025-02-01 00:00:00'),
(4,  'Delta Distribution',     'Ho Chi Minh City', '0901000004', 'sales@deltadist.vn',        'www.deltadist.vn',      'TAX0000004', 'ACTIVE', '2023-02-10 00:00:00', '2025-02-10 00:00:00'),
(5,  'Echo Wholesale',         'Hanoi',            '0901000005', 'order@echowholesale.vn',    'www.echowholesale.vn',  'TAX0000005', 'ACTIVE', '2023-03-01 00:00:00', '2025-03-01 00:00:00'),
(6,  'Foxtrot Logistics',      'Ho Chi Minh City', '0901000006', 'ops@foxtrotlogistics.vn',   'www.foxtrotlogistics.vn','TAX0000006', 'ACTIVE', '2023-03-15 00:00:00', '2025-03-15 00:00:00'),
(7,  'Gamma Partners',         'Da Nang',          '0901000007', 'info@gammapartners.vn',     'www.gammapartners.vn',  'TAX0000007', 'ACTIVE', '2023-04-01 00:00:00', '2025-04-01 00:00:00'),
(8,  'Hotel Imports Inc',      'Ho Chi Minh City', '0902000008', 'contact@hotelimports.vn',   'www.hotelimports.vn',   'TAX0000008', 'ACTIVE', '2023-05-01 00:00:00', '2025-05-01 00:00:00'),
(9,  'Swift Trade Co',         'Hanoi',            '0902000009', 'trade@swifttrade.vn',        'www.swifttrade.vn',     'TAX0000009', 'ACTIVE', '2023-05-15 00:00:00', '2025-05-15 00:00:00'),
(10, 'Reliable Sources Ltd',   'Da Nang',          '0902000010', 'info@reliablesources.vn',   'www.reliablesources.vn','TAX0000010', 'ACTIVE', '2023-06-01 00:00:00', '2025-06-01 00:00:00'),
(11, 'Pacific Supply Group',   'Ho Chi Minh City', '0902000011', 'sales@pacificsupply.vn',    'www.pacificsupply.vn',  'TAX0000011', 'ACTIVE', '2023-06-15 00:00:00', '2025-06-15 00:00:00'),
(12, 'Metro Merchants',        'Hanoi',            '0902000012', 'order@metrotraders.vn',     'www.metrotraders.vn',   'TAX0000012', 'ACTIVE', '2023-07-01 00:00:00', '2025-07-01 00:00:00'),
(13, 'Urban Distributors',     'Ho Chi Minh City', '0902000013', 'info@urbandist.vn',         'www.urbandist.vn',      'TAX0000013', 'ACTIVE', '2023-07-15 00:00:00', '2025-07-15 00:00:00'),
(14, 'Premier Products Co',    'Da Nang',          '0902000014', 'sales@premierproducts.vn',  'www.premierproducts.vn','TAX0000014', 'ACTIVE', '2023-08-01 00:00:00', '2025-08-01 00:00:00'),
(15, 'Summit Supplies',        'Hanoi',            '0902000015', 'contact@summitsupplies.vn', 'www.summitsupplies.vn', 'TAX0000015', 'ACTIVE', '2023-08-15 00:00:00', '2025-08-15 00:00:00'),
(16, 'Valley Trading',         'Ho Chi Minh City', '0903000016', 'hello@valleytrading.vn',    'www.valleytrading.vn',  'TAX0000016', 'ACTIVE', '2023-09-01 00:00:00', '2025-09-01 00:00:00'),
(17, 'Coast Commerce',         'Da Nang',          '0903000017', 'ops@coastcommerce.vn',      'www.coastcommerce.vn',  'TAX0000017', 'ACTIVE', '2023-09-15 00:00:00', '2025-09-15 00:00:00'),
(18, 'Highland Goods',         'Hanoi',            '0903000018', 'info@highlandgoods.vn',     'www.highlandgoods.vn',  'TAX0000018', 'ACTIVE', '2023-10-01 00:00:00', '2025-10-01 00:00:00'),
(19, 'River Trade Partners',   'Ho Chi Minh City', '0903000019', 'sales@rivertrade.vn',       'www.rivertrade.vn',     'TAX0000019', 'ACTIVE', '2023-10-15 00:00:00', '2025-10-15 00:00:00'),
(20, 'Bay Area Distributors',  'Da Nang',          '0903000020', 'contact@baydist.vn',        'www.baydist.vn',        'TAX0000020', 'INACTIVE','2023-11-01 00:00:00', '2025-11-01 00:00:00');

-- ------------------------------------------------------------
-- Products
-- ------------------------------------------------------------
INSERT INTO products (id, created_at, description, expiry_date, image_url, name, price, sku, stock_quantity, reorder_threshold, category_id, warehouse_id) VALUES
(1,  '2025-07-19 18:00:00', 'Latest smartphone',          '2026-01-01 00:00:00', 'products/fef3ec6d-36d1-4cda-8dd4-472647152c20_samsung-galaxy-_main_500_1020.png.webp',                                                 'Smartphone A1',       500.00, 'SKU001',  20,   5, 1, 1),
(2,  '2025-07-30 04:00:00', 'Upgraded smartphone',         '2026-06-01 00:00:00', 'products/bd2a9b2b-0213-4710-986f-11f7a901f256_Du-an-moi-3-2.png',                                                                     'Smartphone A2',       600.00, 'SKU002',  15,   5, 1, 1),
(3,  '2025-10-16 16:00:00', 'Budget smartphone',           '2026-02-01 00:00:00', 'products/36c1f0dc-0740-421b-a768-8d6d098b7320_iphone-17-pro-max_3.webp',                                                              'Smartphone A3',       300.00, 'SKU003',  25,   5, 1, 1),
(16, '2024-11-01 09:00:00', '15-inch high-performance laptop',   '2027-11-01 00:00:00', NULL,                                                                                                                            'Laptop Pro 15',      1200.00, 'SKU016',  12,   3, 1, 1),
(17, '2025-01-15 10:00:00', 'Over-ear noise-cancelling headphones','2027-01-15 00:00:00', NULL,                                                                                                                          'Wireless Headphones',  80.00, 'SKU017',  35,   8, 1, 1),
(18, '2025-03-20 11:00:00', 'Smart watch with health tracking',  '2027-03-20 00:00:00', NULL,                                                                                                                            'Smart Watch S1',      150.00, 'SKU018',  20,   5, 1, 1),
(19, '2024-12-10 09:00:00', '10-inch Android tablet',            '2027-12-10 00:00:00', NULL,                                                                                                                            'Tablet X10',          350.00, 'SKU019',  15,   4, 1, 1),
(20, '2025-02-05 10:00:00', 'Portable Bluetooth speaker 20W',   '2027-02-05 00:00:00', NULL,                                                                                                                             'Bluetooth Speaker',    45.00, 'SKU020',  40,  10, 1, 1),
(21, '2025-01-10 09:00:00', 'Fast-charging USB-C cable and adapter','2027-01-10 00:00:00', NULL,                                                                                                                         'USB-C Charger',        15.00, 'SKU021',  90,  20, 1, 1),
(22, '2025-04-01 10:00:00', 'Mechanical gaming keyboard RGB',   '2027-04-01 00:00:00', NULL,                                                                                                                             'Mechanical Keyboard',  90.00, 'SKU022',  25,   8, 1, 1),
(48, '2025-06-15 09:00:00', 'Ergonomic gaming mouse 16000 DPI', '2027-06-15 00:00:00', NULL,                                                                                                                             'Gaming Mouse',         35.00, 'SKU048',  45,  12, 1, 1),
(4,  '2025-08-30 04:00:00', 'Cotton T-Shirt',              '2026-11-22 15:16:53', 'products/9a2f258b-e239-4636-829f-7975e568879c_download (17).jpg',                                                                     'T-Shirt Basic',        15.00, 'SKU004', 100,  20, 2, 2),
(5,  '2025-11-09 16:00:00', 'Warm winter jacket',           '2026-11-22 15:17:18', 'products/a598644d-487a-4977-b4f5-6c7515fe8e18_download (18).jpg',                                                                    'Jacket Winter',        45.00, 'SKU005',  40,  10, 2, 2),
(23, '2025-01-20 10:00:00', 'Lightweight running shoes',    '2027-01-20 00:00:00', NULL,                                                                                                                                 'Running Shoes',        70.00, 'SKU023',  45,  10, 2, 2),
(24, '2025-02-10 10:00:00', 'Slim-fit denim jeans',         '2027-02-10 00:00:00', NULL,                                                                                                                                 'Slim Jeans',           40.00, 'SKU024',  60,  15, 2, 2),
(25, '2025-04-15 10:00:00', 'Floral summer dress',          '2027-04-15 00:00:00', NULL,                                                                                                                                 'Summer Dress',         35.00, 'SKU025',  55,  15, 2, 2),
(26, '2025-05-01 10:00:00', 'Classic pullover hoodie',      '2027-05-01 00:00:00', NULL,                                                                                                                                 'Hoodie Classic',       50.00, 'SKU026',  70,  15, 2, 2),
(27, '2025-05-20 10:00:00', 'Quick-dry sports shorts',      '2027-05-20 00:00:00', NULL,                                                                                                                                 'Sports Shorts',        20.00, 'SKU027',  80,  20, 2, 2),
(49, '2025-09-01 10:00:00', 'Classic stonewash denim jacket','2027-09-01 00:00:00', NULL,                                                                                                                                 'Denim Jacket',         60.00, 'SKU049',  35,   8, 2, 2),
(6,  '2025-10-24 22:00:00', 'High speed blender',           '2027-01-01 00:00:00', 'products/52963d8f-c8d9-4e3d-a002-f1b44fa45a31_3aa9f4d3-0b10-432e-b67f427c81c46ba7-396476f2-3200000395-20250618-092613.webp',        'Blender X',            80.00, 'SKU006',  30,   8, 3, 3),
(7,  '2025-11-04 15:00:00', '5-piece cookware set',          '2026-11-22 15:18:25', 'products/94da0d4a-9bb0-4239-b096-7d33ce3496d5_best-cookware-sets_LEDE_021925_1398_VOG_Badge_final.webp',                            'Cookware Set',         60.00, 'SKU007',  25,   8, 3, 3),
(28, '2025-03-10 10:00:00', 'Professional ceramic knife set 6-piece','2027-03-10 00:00:00', NULL,                                                                                                                        'Ceramic Knife Set',    55.00, 'SKU028',  30,   8, 3, 3),
(29, '2025-04-05 10:00:00', '1.8L fuzzy logic rice cooker', '2027-04-05 00:00:00', NULL,                                                                                                                                  'Rice Cooker 1.8L',     45.00, 'SKU029',  35,   8, 3, 3),
(30, '2025-05-12 10:00:00', '12-cup programmable coffee maker','2027-05-12 00:00:00', NULL,                                                                                                                               'Coffee Maker Pro',     85.00, 'SKU030',  20,   5, 3, 3),
(31, '2025-02-20 10:00:00', 'Extra-large bamboo cutting board','2027-02-20 00:00:00', NULL,                                                                                                                               'Cutting Board XL',     18.00, 'SKU031',  50,  15, 3, 3),
(8,  '2025-11-09 06:00:00', 'Classic wrist watch',           '2026-11-22 15:18:37', 'products/7911592a-4273-4e12-9fe6-d991d1b006f7_dong-ho-jacob-co-astronomia-art-dragon-rose-gold-sky-at112-40-dr-sd-a-phien-ban-duy-nhat.jpg', 'Watch Classic',    50.00, 'SKU008',  75,  15, 4, 4),
(9,  '2025-10-24 03:00:00', 'Silver bracelet',               '2026-11-22 15:18:48', 'products/5d5d831c-43f5-42c1-9406-a0aea5eede0f_71kFO60WnnL._AC_UY1100_.jpg',                                                         'Bracelet Silver',      30.00, 'SKU009',  60,  15, 4, 4),
(32, '2025-03-15 10:00:00', 'UV400 polarised sunglasses',    '2027-03-15 00:00:00', NULL,                                                                                                                                 'Sunglasses UV400',     35.00, 'SKU032',  55,  15, 4, 4),
(33, '2025-03-22 10:00:00', 'Genuine leather bifold wallet', '2027-03-22 00:00:00', NULL,                                                                                                                                 'Leather Wallet',       25.00, 'SKU033',  70,  20, 4, 4),
(34, '2025-05-30 10:00:00', 'Lightweight canvas backpack 20L','2027-05-30 00:00:00', NULL,                                                                                                                                'Canvas Backpack',      40.00, 'SKU034',  45,  10, 4, 4),
(35, '2025-06-10 10:00:00', '18K gold plated necklace',      '2027-06-10 00:00:00', NULL,                                                                                                                                 'Gold Necklace',        65.00, 'SKU035',  30,   8, 4, 4),
(10, '2025-11-05 00:00:00', 'Toy racing car',                '2026-11-22 15:18:55', 'products/9c110ef6-77e5-4293-a5d0-d3bc47abdbb6_images (1).jpg',                                                                      'Toy Car',              10.00, 'SKU010', 120,  25, 5, 4),
(11, '2025-07-28 12:00:00', 'Lego building set',             '2026-11-22 15:19:03', 'products/c16c1017-5806-4b4e-b1f4-765499ce810a_81QUSmkau7L.jpg',                                                                     'Lego Set',             55.00, 'SKU011',  35,  10, 5, 4),
(36, '2025-04-10 10:00:00', 'Remote control car 2.4GHz',     '2027-04-10 00:00:00', NULL,                                                                                                                                 'Remote Control Car',   35.00, 'SKU036',  40,  10, 5, 4),
(37, '2025-04-20 10:00:00', '200-piece wooden jigsaw puzzle', '2027-04-20 00:00:00', NULL,                                                                                                                                 'Wooden Puzzle',        22.00, 'SKU037',  55,  15, 5, 4),
(38, '2025-10-01 10:00:00', 'Classic strategy board game',   '2027-10-01 00:00:00', NULL,                                                                                                                                  'Board Game Classic',   30.00, 'SKU038',  45,  12, 5, 4),
(12, '2025-08-12 10:00:00', 'Professional basketball',       '2026-11-22 15:19:09', 'products/598f1b57-4c0e-4ae8-ab8c-729b368fd847_a78425d8-6944-4644-ae09-c065ee8e3156.fe6212bc476847fd8b3354d2a28745c6.webp',          'Basketball Pro',       20.00, 'SKU012',  45,  10, 6, 5),
(13, '2025-08-12 16:00:00', 'Carbon tennis racket',          '2026-11-22 15:19:15', 'products/b98f3752-5bd0-425d-83b4-61959e03bb3e_ATR32-medium__61580.jpg',                                                             'Tennis Racket',        75.00, 'SKU013',  20,   5, 6, 5),
(39, '2025-04-08 10:00:00', 'Non-slip premium yoga mat 6mm', '2027-04-08 00:00:00', NULL,                                                                                                                                  'Yoga Mat Premium',     28.00, 'SKU039',  40,  10, 6, 5),
(40, '2025-03-18 10:00:00', 'Cast iron dumbbell set 2x5kg',  '2027-03-18 00:00:00', NULL,                                                                                                                                  'Dumbbell Set 5kg',     32.00, 'SKU040',  30,   8, 6, 5),
(41, '2025-08-25 10:00:00', 'Adjustable steel jump rope',    '2027-08-25 00:00:00', NULL,                                                                                                                                  'Jump Rope Steel',      12.00, 'SKU041',  60,  15, 6, 5),
(42, '2025-10-05 10:00:00', 'FIFA-approved size 5 soccer ball','2027-10-05 00:00:00', NULL,                                                                                                                               'Soccer Ball Pro',       25.00, 'SKU042',  35,  10, 6, 5),
(14, '2025-07-24 18:00:00', 'Wall-mounted lamp',             '2026-11-22 15:19:21', 'products/48c19b61-977a-474b-9a88-abef7689fd16_wall_lights_for_living_room_uk.webp',                                                 'Wall Lamp',            25.00, 'SKU014',  60,  15, 7, 5),
(15, '2025-07-28 08:00:00', 'Wooden picture frame',          '2026-11-22 15:19:30', 'products/4af2de9f-9741-49ff-8a7c-b999f0ed98eb_old-picture-frame-with-clipping-path-D9KDWF.jpg',                                     'Picture Frame',        12.00, 'SKU015',  80,  20, 7, 5),
(43, '2025-07-01 10:00:00', 'Hand-painted ceramic flower vase','2027-07-01 00:00:00', NULL,                                                                                                                               'Ceramic Vase',         22.00, 'SKU043',  45,  12, 7, 5),
(44, '2025-07-15 10:00:00', 'Round wall mirror 60cm diameter','2027-07-15 00:00:00', NULL,                                                                                                                                 'Round Mirror',         45.00, 'SKU044',  25,   8, 7, 5),
(45, '2025-08-01 10:00:00', 'Set of 4 scented soy candles',  '2027-08-01 00:00:00', NULL,                                                                                                                                  'Scented Candle Set',   18.00, 'SKU045',  70,  20, 7, 5),
(46, '2025-08-10 10:00:00', 'Velvet decorative throw cushion','2027-08-10 00:00:00', NULL,                                                                                                                                 'Throw Cushion',        15.00, 'SKU046',  80,  20, 7, 5),
(47, '2025-09-05 10:00:00', 'Natural bamboo area rug 2x3m',  '2027-09-05 00:00:00', NULL,                                                                                                                                  'Bamboo Rug 2x3m',      55.00, 'SKU047',  20,   5, 7, 5),
(50, '2025-11-20 10:00:00', 'Modular bamboo desk organizer', '2027-11-20 00:00:00', NULL,                                                                                                                                  'Desk Organizer',       20.00, 'SKU050',  65,  15, 7, 5);

-- ------------------------------------------------------------
-- Product Warehouses (NEW TABLE: derived from products stock info)
-- ------------------------------------------------------------
INSERT INTO product_warehouses (product_id, warehouse_id, quantity) VALUES
(1, 1, 20), (2, 1, 15), (3, 1, 25), (16, 1, 12), (17, 1, 35), (18, 1, 20), (19, 1, 15), (20, 1, 40), (21, 1, 90), (22, 1, 25), (48, 1, 45),
(4, 2, 100), (5, 2, 40), (23, 2, 45), (24, 2, 60), (25, 2, 55), (26, 2, 70), (27, 2, 80), (49, 2, 35),
(6, 3, 30), (7, 3, 25), (28, 3, 30), (29, 3, 35), (30, 3, 20), (31, 3, 50),
(8, 4, 75), (9, 4, 60), (32, 4, 55), (33, 4, 70), (34, 4, 45), (35, 4, 30), (10, 4, 120), (11, 4, 35), (36, 4, 40), (37, 4, 55), (38, 4, 45),
(12, 5, 45), (13, 5, 20), (39, 5, 40), (40, 5, 30), (41, 5, 60), (42, 5, 35), (14, 5, 60), (15, 5, 80), (43, 5, 45), (44, 5, 25), (45, 5, 70), (46, 5, 80), (47, 5, 20), (50, 5, 65);

-- ------------------------------------------------------------
-- Product Suppliers 
-- ------------------------------------------------------------
INSERT INTO product_suppliers (product_id, supplier_id, unit_cost, is_preferred, created_at) VALUES
(1,  1,  270.00, TRUE,  '2024-06-01 00:00:00'), (1,  8,  285.00, FALSE, '2024-06-01 00:00:00'),
(2,  1,  320.00, TRUE,  '2024-06-01 00:00:00'), (2,  13, 340.00, FALSE, '2024-06-01 00:00:00'),
(3,  8,  160.00, TRUE,  '2024-06-01 00:00:00'), (3,  13, 175.00, FALSE, '2024-06-01 00:00:00'),
(16, 1,  700.00, TRUE,  '2024-06-01 00:00:00'), (16, 8,  720.00, FALSE, '2024-06-01 00:00:00'), (16, 13, 750.00, FALSE, '2024-11-01 00:00:00'),
(17, 8,   40.00, TRUE,  '2025-01-15 00:00:00'), (17, 13,  43.00, FALSE, '2025-01-15 00:00:00'),
(18, 1,   80.00, TRUE,  '2025-03-20 00:00:00'), (18, 13,  85.00, FALSE, '2025-03-20 00:00:00'),
(19, 1,  185.00, TRUE,  '2024-12-10 00:00:00'), (19, 13, 195.00, FALSE, '2024-12-10 00:00:00'), (19, 8, 200.00, FALSE, '2025-02-01 00:00:00'),
(20, 8,   22.00, TRUE,  '2025-02-05 00:00:00'), (20, 1,   24.00, FALSE, '2025-02-05 00:00:00'),
(21, 8,    6.00, TRUE,  '2025-01-10 00:00:00'), (21, 13,   7.00, FALSE, '2025-01-10 00:00:00'),
(22, 8,   45.00, TRUE,  '2025-04-01 00:00:00'), (22, 13,  48.00, FALSE, '2025-04-01 00:00:00'),
(48, 8,   16.00, TRUE,  '2025-06-15 00:00:00'), (48, 1,   18.00, FALSE, '2025-06-15 00:00:00'),
(4,  2,    7.00, TRUE,  '2024-06-01 00:00:00'), (4,  9,    8.00, FALSE, '2024-06-01 00:00:00'),
(5,  2,   22.00, TRUE,  '2024-06-01 00:00:00'), (5,  14,  25.00, FALSE, '2024-06-01 00:00:00'),
(23, 5,   35.00, TRUE,  '2025-01-20 00:00:00'), (23, 14,  38.00, FALSE, '2025-01-20 00:00:00'),
(24, 2,   18.00, TRUE,  '2025-02-10 00:00:00'), (24, 9,   20.00, FALSE, '2025-02-10 00:00:00'),
(25, 9,   17.00, TRUE,  '2025-04-15 00:00:00'), (25, 14,  19.00, FALSE, '2025-04-15 00:00:00'),
(26, 2,   24.00, TRUE,  '2025-05-01 00:00:00'), (26, 9,   26.00, FALSE, '2025-05-01 00:00:00'),
(27, 5,    9.00, TRUE,  '2025-05-20 00:00:00'), (27, 9,   10.00, FALSE, '2025-05-20 00:00:00'),
(49, 2,   29.00, TRUE,  '2025-09-01 00:00:00'), (49, 14,  31.00, FALSE, '2025-09-01 00:00:00'),
(6,  3,   42.00, TRUE,  '2024-06-01 00:00:00'), (6,  15,  45.00, FALSE, '2024-06-01 00:00:00'),
(7,  10,  32.00, TRUE,  '2024-06-01 00:00:00'), (7,  15,  35.00, FALSE, '2024-06-01 00:00:00'),
(28, 10,  28.00, TRUE,  '2025-03-10 00:00:00'), (28, 3,   30.00, FALSE, '2025-03-10 00:00:00'),
(29, 10,  22.00, TRUE,  '2025-04-05 00:00:00'), (29, 15,  24.00, FALSE, '2025-04-05 00:00:00'),
(30, 15,  43.00, TRUE,  '2025-05-12 00:00:00'), (30, 10,  45.00, FALSE, '2025-05-12 00:00:00'),
(31, 3,    7.00, TRUE,  '2025-02-20 00:00:00'), (31, 10,   8.00, FALSE, '2025-02-20 00:00:00'),
(8,  4,   25.00, TRUE,  '2024-06-01 00:00:00'), (8,  11,  27.00, FALSE, '2024-06-01 00:00:00'),
(9,  4,   15.00, TRUE,  '2024-06-01 00:00:00'), (9,  16,  17.00, FALSE, '2024-06-01 00:00:00'),
(32, 11,  16.00, TRUE,  '2025-03-15 00:00:00'), (32, 4,   18.00, FALSE, '2025-03-15 00:00:00'),
(33, 7,   12.00, TRUE,  '2025-03-22 00:00:00'), (33, 11,  13.00, FALSE, '2025-03-22 00:00:00'),
(34, 16,  19.00, TRUE,  '2025-05-30 00:00:00'), (34, 11,  21.00, FALSE, '2025-05-30 00:00:00'),
(35, 4,   30.00, TRUE,  '2025-06-10 00:00:00'), (35, 7,   33.00, FALSE, '2025-06-10 00:00:00'),
(10, 1,    4.00, TRUE,  '2024-06-01 00:00:00'), (10, 12,   5.00, FALSE, '2024-06-01 00:00:00'),
(11, 5,   28.00, TRUE,  '2024-06-01 00:00:00'), (11, 12,  30.00, FALSE, '2024-06-01 00:00:00'),
(36, 12,  16.00, TRUE,  '2025-04-10 00:00:00'), (36, 5,   18.00, FALSE, '2025-04-10 00:00:00'),
(37, 12,  10.00, TRUE,  '2025-04-20 00:00:00'), (37, 5,   11.00, FALSE, '2025-04-20 00:00:00'),
(38, 1,   14.00, TRUE,  '2025-10-01 00:00:00'), (38, 12,  15.00, FALSE, '2025-10-01 00:00:00'),
(12, 6,   10.00, TRUE,  '2024-06-01 00:00:00'), (12, 17,  11.00, FALSE, '2024-06-01 00:00:00'),
(13, 6,   38.00, TRUE,  '2024-06-01 00:00:00'), (13, 17,  40.00, FALSE, '2024-06-01 00:00:00'),
(39, 17,  13.00, TRUE,  '2025-04-08 00:00:00'), (39, 6,   14.00, FALSE, '2025-04-08 00:00:00'),
(40, 17,  15.00, TRUE,  '2025-03-18 00:00:00'), (40, 6,   16.00, FALSE, '2025-03-18 00:00:00'),
(41, 6,    5.00, TRUE,  '2025-08-25 00:00:00'), (41, 17,   6.00, FALSE, '2025-08-25 00:00:00'),
(42, 6,   11.00, TRUE,  '2025-10-05 00:00:00'), (42, 17,  12.00, FALSE, '2025-10-05 00:00:00'),
(14, 7,   12.00, TRUE,  '2024-06-01 00:00:00'), (14, 18,  13.00, FALSE, '2024-06-01 00:00:00'),
(15, 3,    5.00, TRUE,  '2024-06-01 00:00:00'), (15, 18,   6.00, FALSE, '2024-06-01 00:00:00'),
(43, 7,   10.00, TRUE,  '2025-07-01 00:00:00'), (43, 18,  11.00, FALSE, '2025-07-01 00:00:00'),
(44, 18,  21.00, TRUE,  '2025-07-15 00:00:00'), (44, 7,   23.00, FALSE, '2025-07-15 00:00:00'),
(45, 3,    8.00, TRUE,  '2025-08-01 00:00:00'), (45, 7,    9.00, FALSE, '2025-08-01 00:00:00'),
(46, 18,   7.00, TRUE,  '2025-08-10 00:00:00'), (46, 3,    8.00, FALSE, '2025-08-10 00:00:00'),
(47, 18,  27.00, TRUE,  '2025-09-05 00:00:00'), (47, 7,   29.00, FALSE, '2025-09-05 00:00:00'),
(50, 3,    9.00, TRUE,  '2025-11-20 00:00:00'), (50, 18,  10.00, FALSE, '2025-11-20 00:00:00');

-- ------------------------------------------------------------
-- Transactions (updated_at)
-- ------------------------------------------------------------
INSERT INTO transactions (id, created_at, description, note, status, total_price, total_products, transaction_type, updated_at, product_id, supplier_id, user_id) VALUES
(8,  '2025-01-05 09:00:00', 'Initial laptop stock',     'Bulk purchase',    'COMPLETED',  6000.00,  5, 'PURCHASE',          '2025-01-05 17:00:00', 16, 1,    1),
(9,  '2025-01-08 10:30:00', 'Clothing restock',         'Monthly order',    'COMPLETED',   750.00, 50, 'PURCHASE',          '2025-01-08 18:00:00',  4, 2,    1),
(10, '2025-01-12 14:00:00', 'Smartphone sale',          'Online order',     'COMPLETED',  1000.00,  2, 'SALE',              '2025-01-12 14:00:00',  1, NULL, 2),
(11, '2025-01-15 11:00:00', 'T-shirt sale',             'Retail store',     'COMPLETED',   150.00, 10, 'SALE',              '2025-01-15 11:00:00',  4, NULL, 2),
(12, '2025-01-18 09:00:00', 'Headphones restock',       'Regular order',    'COMPLETED',  1600.00, 20, 'PURCHASE',          '2025-01-18 17:00:00', 17, 8,    1),
(13, '2025-01-22 15:00:00', 'Blender sale',             'Walk-in customer', 'COMPLETED',   240.00,  3, 'SALE',              '2025-01-22 15:00:00',  6, NULL, 2),
(14, '2025-01-25 08:00:00', 'Footwear restock',         'Seasonal stock',   'COMPLETED',  2100.00, 30, 'PURCHASE',          '2025-01-25 16:00:00', 23, 5,    1),
(15, '2025-01-29 16:00:00', 'Toy sale',                 'Bulk retail',      'COMPLETED',   150.00, 15, 'SALE',              '2025-01-29 16:00:00', 10, NULL, 2),
(16, '2025-02-03 09:00:00', 'Tablet inventory',         'Bulk purchase',    'COMPLETED',  3500.00, 10, 'PURCHASE',          '2025-02-03 17:00:00', 19, 13,   1),
(17, '2025-02-07 10:00:00', 'Denim restock',            'Monthly order',    'COMPLETED',  1600.00, 40, 'PURCHASE',          '2025-02-07 18:00:00', 24, 9,    1),
(18, '2025-02-10 14:00:00', 'Smartphone A2 sale',       'Online order',     'COMPLETED',   600.00,  1, 'SALE',              '2025-02-10 14:00:00',  2, NULL, 2),
(19, '2025-02-14 11:00:00', 'Laptop sale',              'Corporate order',  'COMPLETED',  2400.00,  2, 'SALE',              '2025-02-14 11:00:00', 16, NULL, 2),
(20, '2025-02-18 09:00:00', 'Kitchen tools restock',    'Regular order',    'COMPLETED',  1100.00, 20, 'PURCHASE',          '2025-02-18 17:00:00', 28, 10,   1),
(21, '2025-02-20 15:00:00', 'Watch sale',               'Walk-in customer', 'COMPLETED',   250.00,  5, 'SALE',              '2025-02-20 15:00:00',  8, NULL, 2),
(22, '2025-02-24 10:00:00', 'Defective blender return', 'Quality issue',    'PROCESSING',    0.00,  2, 'RETURN_TO_SUPPLIER','2025-02-24 10:00:00',  6, 3,    1),
(23, '2025-02-27 16:00:00', 'Running shoes sale',       'Retail store',     'COMPLETED',   560.00,  8, 'SALE',              '2025-02-27 16:00:00', 23, NULL, 2),
(24, '2025-03-04 09:00:00', 'Speaker restock',          'Bulk purchase',    'COMPLETED',  1350.00, 30, 'PURCHASE',          '2025-03-04 17:00:00', 20, 8,    1),
(25, '2025-03-07 10:00:00', 'Sunglasses restock',       'Seasonal stock',   'COMPLETED',  1400.00, 40, 'PURCHASE',          '2025-03-07 18:00:00', 32, 11,   1),
(26, '2025-03-10 14:00:00', 'T-shirt sale',             'Online order',     'COMPLETED',   300.00, 20, 'SALE',              '2025-03-10 14:00:00',  4, NULL, 2),
(27, '2025-03-13 11:00:00', 'Lego sale',                'Walk-in customer', 'COMPLETED',   330.00,  6, 'SALE',              '2025-03-13 11:00:00', 11, NULL, 2),
(28, '2025-03-17 09:00:00', 'Sports equipment restock', 'Regular order',    'COMPLETED',   640.00, 20, 'PURCHASE',          '2025-03-17 17:00:00', 40, 17,   1),
(29, '2025-03-20 15:00:00', 'Headphones sale',          'Online order',     'COMPLETED',   320.00,  4, 'SALE',              '2025-03-20 15:00:00', 17, NULL, 2),
(30, '2025-03-24 08:00:00', 'Wallet restock',           'Monthly order',    'COMPLETED',   750.00, 30, 'PURCHASE',          '2025-03-24 16:00:00', 33, 7,    1),
(31, '2025-03-28 16:00:00', 'Home decor sale',          'Retail store',     'COMPLETED',   200.00,  8, 'SALE',              '2025-03-28 16:00:00', 14, NULL, 2),
(32, '2025-04-03 09:00:00', 'Toy restock',              'Bulk purchase',    'COMPLETED',   875.00, 25, 'PURCHASE',          '2025-04-03 17:00:00', 36, 12,   1),
(33, '2025-04-07 10:00:00', 'Sports accessories restock','Regular order',   'COMPLETED',   700.00, 25, 'PURCHASE',          '2025-04-07 18:00:00', 39, 17,   1),
(34, '2025-04-10 14:00:00', 'Smartphone A3 sale',       'Online order',     'COMPLETED',   900.00,  3, 'SALE',              '2025-04-10 14:00:00',  3, NULL, 2),
(35, '2025-04-14 11:00:00', 'Tablet sale',              'Corporate order',  'COMPLETED',  1050.00,  3, 'SALE',              '2025-04-14 11:00:00', 19, NULL, 2),
(36, '2025-04-17 09:00:00', 'Candle set restock',       'Seasonal stock',   'COMPLETED',   900.00, 50, 'PURCHASE',          '2025-04-17 17:00:00', 45, 7,    1),
(37, '2025-04-21 15:00:00', 'Bracelet sale',            'Retail store',     'COMPLETED',   300.00, 10, 'SALE',              '2025-04-21 15:00:00',  9, NULL, 2),
(38, '2025-04-25 10:00:00', 'Defective tablet return',  'Customer complaint','PROCESSING',   0.00,  1, 'RETURN_TO_SUPPLIER','2025-04-25 10:00:00', 19, 13,   1),
(39, '2025-04-29 16:00:00', 'Jeans sale',               'Online order',     'COMPLETED',   480.00, 12, 'SALE',              '2025-04-29 16:00:00', 24, NULL, 2),
(40, '2025-05-05 09:00:00', 'Keyboard restock',         'Bulk purchase',    'COMPLETED',  1350.00, 15, 'PURCHASE',          '2025-05-05 17:00:00', 22, 8,    1),
(41, '2025-05-08 10:00:00', 'Summer dress restock',     'Seasonal stock',   'COMPLETED',  1400.00, 40, 'PURCHASE',          '2025-05-08 18:00:00', 25, 9,    1),
(42, '2025-05-12 14:00:00', 'Speaker sale',             'Online order',     'COMPLETED',   270.00,  6, 'SALE',              '2025-05-12 14:00:00', 20, NULL, 2),
(43, '2025-05-15 11:00:00', 'Knife set sale',           'Walk-in customer', 'COMPLETED',   275.00,  5, 'SALE',              '2025-05-15 11:00:00', 28, NULL, 2),
(44, '2025-05-19 09:00:00', 'Puzzle restock',           'Regular order',    'COMPLETED',   660.00, 30, 'PURCHASE',          '2025-05-19 17:00:00', 37, 12,   1),
(45, '2025-05-22 15:00:00', 'Basketball sale',          'Retail store',     'COMPLETED',   160.00,  8, 'SALE',              '2025-05-22 15:00:00', 12, NULL, 2),
(46, '2025-05-26 08:00:00', 'Coffee maker restock',     'Monthly order',    'COMPLETED',  1275.00, 15, 'PURCHASE',          '2025-05-26 16:00:00', 30, 10,   1),
(47, '2025-05-30 16:00:00', 'Keyboard sale',            'Online order',     'COMPLETED',   270.00,  3, 'SALE',              '2025-05-30 16:00:00', 22, NULL, 2),
(48, '2025-06-03 09:00:00', 'Smart watch restock',      'Bulk purchase',    'COMPLETED',  2250.00, 15, 'PURCHASE',          '2025-06-03 17:00:00', 18, 13,   1),
(49, '2025-06-06 10:00:00', 'Hoodie restock',           'Seasonal stock',   'COMPLETED',  2000.00, 40, 'PURCHASE',          '2025-06-06 18:00:00', 26, 14,   1),
(50, '2025-06-10 14:00:00', 'Sunglasses sale',          'Summer sale',      'COMPLETED',   525.00, 15, 'SALE',              '2025-06-10 14:00:00', 32, NULL, 2),
(51, '2025-06-13 11:00:00', 'Smart watch sale',         'Online order',     'COMPLETED',   600.00,  4, 'SALE',              '2025-06-13 11:00:00', 18, NULL, 2),
(52, '2025-06-17 09:00:00', 'Mirror restock',           'Regular order',    'COMPLETED',   675.00, 15, 'PURCHASE',          '2025-06-17 17:00:00', 44, 18,   1),
(53, '2025-06-20 15:00:00', 'Wallet sale',              'Retail store',     'COMPLETED',   300.00, 12, 'SALE',              '2025-06-20 15:00:00', 33, NULL, 2),
(54, '2025-06-24 08:00:00', 'Backpack restock',         'Monthly order',    'COMPLETED',   800.00, 20, 'PURCHASE',          '2025-06-24 16:00:00', 34, 16,   1),
(55, '2025-06-28 16:00:00', 'RC car sale',              'Walk-in customer', 'COMPLETED',   280.00,  8, 'SALE',              '2025-06-28 16:00:00', 36, NULL, 2),
(1,  '2025-07-24 17:00:00', 'Purchase stock',           'Initial load',     'COMPLETED',  2500.00,  5, 'PURCHASE',          '2025-11-15 19:49:44',  1, 1,    1),
(3,  '2025-07-30 05:00:00', 'Return to supplier',       'Damaged product',  'PROCESSING',   45.00,  1, 'RETURN_TO_SUPPLIER','2025-11-15 19:49:44',  5, 3,    1),
(56, '2025-07-05 09:00:00', 'Charger restock',          'Bulk purchase',    'COMPLETED',   600.00, 40, 'PURCHASE',          '2025-07-05 17:00:00', 21, 8,    1),
(57, '2025-07-09 10:00:00', 'Sports shorts restock',    'Summer stock',     'COMPLETED',  1000.00, 50, 'PURCHASE',          '2025-07-09 18:00:00', 27, 5,    1),
(58, '2025-07-13 14:00:00', 'Summer dress sale',        'Summer sale',      'COMPLETED',   525.00, 15, 'SALE',              '2025-07-13 14:00:00', 25, NULL, 2),
(59, '2025-07-17 11:00:00', 'Hoodie sale',              'Online order',     'COMPLETED',   500.00, 10, 'SALE',              '2025-07-17 11:00:00', 26, NULL, 2),
(60, '2025-07-21 15:00:00', 'Speaker sale',             'Retail store',     'COMPLETED',   225.00,  5, 'SALE',              '2025-07-21 15:00:00', 20, NULL, 2),
(61, '2025-07-27 16:00:00', 'Puzzle sale',              'Walk-in customer', 'COMPLETED',   220.00, 10, 'SALE',              '2025-07-27 16:00:00', 37, NULL, 2),
(2,  '2025-08-03 02:00:00', 'Sale to customer',         'Online order',     'COMPLETED',   600.00,  1, 'SALE',              '2025-11-15 19:49:44',  3, 2,    2),
(62, '2025-08-04 09:00:00', 'Jewelry restock',          'Bulk purchase',    'COMPLETED',  1300.00, 20, 'PURCHASE',          '2025-08-04 17:00:00', 35, 4,    1),
(63, '2025-08-08 10:00:00', 'Vase restock',             'Regular order',    'COMPLETED',   550.00, 25, 'PURCHASE',          '2025-08-08 18:00:00', 43, 7,    1),
(64, '2025-08-11 14:00:00', 'Smart watch sale',         'Online order',     'COMPLETED',   450.00,  3, 'SALE',              '2025-08-11 14:00:00', 18, NULL, 2),
(65, '2025-08-15 11:00:00', 'Backpack sale',            'Back to school',   'COMPLETED',   320.00,  8, 'SALE',              '2025-08-15 11:00:00', 34, NULL, 2),
(66, '2025-08-19 09:00:00', 'Rice cooker restock',      'Monthly order',    'COMPLETED',   900.00, 20, 'PURCHASE',          '2025-08-19 17:00:00', 29, 10,   1),
(67, '2025-08-22 15:00:00', 'Yoga mat sale',            'Retail store',     'COMPLETED',   168.00,  6, 'SALE',              '2025-08-22 15:00:00', 39, NULL, 2),
(68, '2025-08-27 16:00:00', 'Charger sale',             'Online order',     'COMPLETED',   300.00, 20, 'SALE',              '2025-08-27 16:00:00', 21, NULL, 2),
(69, '2025-09-02 09:00:00', 'Gaming mouse restock',     'Bulk purchase',    'COMPLETED',   875.00, 25, 'PURCHASE',          '2025-09-02 17:00:00', 48, 8,    1),
(70, '2025-09-05 10:00:00', 'Cushion restock',          'Regular order',    'COMPLETED',   750.00, 50, 'PURCHASE',          '2025-09-05 18:00:00', 46, 18,   1),
(71, '2025-09-09 14:00:00', 'Tablet sale',              'Corporate order',  'COMPLETED',   700.00,  2, 'SALE',              '2025-09-09 14:00:00', 19, NULL, 2),
(72, '2025-09-12 11:00:00', 'Necklace sale',            'Retail store',     'COMPLETED',   325.00,  5, 'SALE',              '2025-09-12 11:00:00', 35, NULL, 2),
(73, '2025-09-16 09:00:00', 'Jump rope restock',        'Monthly order',    'COMPLETED',   360.00, 30, 'PURCHASE',          '2025-09-16 17:00:00', 41, 6,    1),
(74, '2025-09-19 15:00:00', 'Coffee maker sale',        'Walk-in customer', 'COMPLETED',   340.00,  4, 'SALE',              '2025-09-19 15:00:00', 30, NULL, 2),
(75, '2025-09-23 10:00:00', 'Damaged rice cooker return','Transit damage',  'COMPLETED',     0.00,  3, 'RETURN_TO_SUPPLIER','2025-09-23 10:00:00', 29, 10,   1),
(76, '2025-09-27 16:00:00', 'Gaming mouse sale',        'Online order',     'COMPLETED',   210.00,  6, 'SALE',              '2025-09-27 16:00:00', 48, NULL, 2),
(4,  '2025-10-01 05:00:00', 'Purchase stock',           'Restock toys',     'COMPLETED',   200.00, 20, 'PURCHASE',          '2025-11-15 19:49:44', 10, 4,    1),
(6,  '2025-10-22 22:00:00', 'Purchase restock',         'Sports equipment', 'COMPLETED',   400.00, 10, 'PURCHASE',          '2025-11-15 19:49:44', 12, 6,    1),
(77, '2025-10-03 09:00:00', 'Denim jacket restock',     'Autumn stock',     'COMPLETED',  1200.00, 20, 'PURCHASE',          '2025-10-03 17:00:00', 49, 2,    1),
(78, '2025-10-08 14:00:00', 'Hoodie sale',              'Autumn sale',      'COMPLETED',   400.00,  8, 'SALE',              '2025-10-08 14:00:00', 26, NULL, 2),
(79, '2025-10-13 09:00:00', 'Soccer ball restock',      'Sports season',    'COMPLETED',   500.00, 20, 'PURCHASE',          '2025-10-13 17:00:00', 42, 6,    1),
(80, '2025-10-17 11:00:00', 'Tennis racket sale',       'Retail store',     'COMPLETED',   300.00,  4, 'SALE',              '2025-10-17 11:00:00', 13, NULL, 2),
(81, '2025-10-28 15:00:00', 'Dumbbell sale',            'Online order',     'COMPLETED',   160.00,  5, 'SALE',              '2025-10-28 15:00:00', 40, NULL, 2),
(82, '2025-10-31 10:00:00', 'Defective RC car return',  'Defective unit',   'PROCESSING',    0.00,  2, 'RETURN_TO_SUPPLIER','2025-10-31 10:00:00', 36, 12,   1),
(5,  '2025-11-05 16:00:00', 'Sale to customer',         'Retail store',     'COMPLETED',   150.00,  3, 'SALE',              '2025-11-15 19:49:44',  8, 5,    2),
(7,  '2025-11-04 15:00:00', 'Return from customer',     'Customer refund',  'PENDING',      25.00,  1, 'RETURN_TO_SUPPLIER','2025-11-15 20:18:30', 14, 7,    2),
(83, '2025-11-03 09:00:00', 'Bamboo rug restock',       'Bulk purchase',    'COMPLETED',   825.00, 15, 'PURCHASE',          '2025-11-03 17:00:00', 47, 18,   1),
(84, '2025-11-07 10:00:00', 'Board game restock',       'Holiday stock',    'COMPLETED',   750.00, 25, 'PURCHASE',          '2025-11-07 18:00:00', 38, 12,   1),
(85, '2025-11-12 14:00:00', 'Denim jacket sale',        'Autumn sale',      'COMPLETED',   300.00,  5, 'SALE',              '2025-11-12 14:00:00', 49, NULL, 2),
(86, '2025-11-17 11:00:00', 'Bamboo rug sale',          'Retail store',     'COMPLETED',   165.00,  3, 'SALE',              '2025-11-17 11:00:00', 47, NULL, 2),
(87, '2025-11-25 15:00:00', 'Mirror sale',              'Home decor',       'COMPLETED',   180.00,  4, 'SALE',              '2025-11-25 15:00:00', 44, NULL, 2),
(88, '2025-11-28 16:00:00', 'Board game sale',          'Holiday gift',     'COMPLETED',   240.00,  8, 'SALE',              '2025-11-28 16:00:00', 38, NULL, 2),
(89, '2025-12-02 09:00:00', 'Laptop holiday restock',   'Holiday bulk',     'COMPLETED',  9600.00,  8, 'PURCHASE',          '2025-12-02 17:00:00', 16, 1,    1),
(90, '2025-12-05 10:00:00', 'Smart watch holiday stock','Holiday stock',    'COMPLETED',  1500.00, 10, 'PURCHASE',          '2025-12-05 18:00:00', 18, 13,   1),
(91, '2025-12-08 14:00:00', 'Laptop holiday sale',      'Holiday sale',     'COMPLETED',  3600.00,  3, 'SALE',              '2025-12-08 14:00:00', 16, NULL, 2),
(92, '2025-12-11 11:00:00', 'Smart watch sale',         'Holiday gift',     'COMPLETED',   750.00,  5, 'SALE',              '2025-12-11 11:00:00', 18, NULL, 2),
(93, '2025-12-15 15:00:00', 'Lego Christmas sale',      'Christmas gift',   'COMPLETED',   550.00, 10, 'SALE',              '2025-12-15 15:00:00', 11, NULL, 2),
(94, '2025-12-18 16:00:00', 'Board game Christmas sale','Christmas gift',   'COMPLETED',   360.00, 12, 'SALE',              '2025-12-18 16:00:00', 38, NULL, 2),
(95, '2025-12-22 09:00:00', 'Desk organizer restock',   'Year-end restock', 'COMPLETED',   600.00, 30, 'PURCHASE',          '2025-12-22 17:00:00', 50, 18,   1),
(96, '2025-12-26 15:00:00', 'Candle set sale',          'Holiday gift',     'COMPLETED',   360.00, 20, 'SALE',              '2025-12-26 15:00:00', 45, NULL, 2),
(97,  '2026-01-05 09:00:00', 'Cutting board restock',  'New year stock',   'COMPLETED',   540.00, 30, 'PURCHASE',          '2026-01-05 17:00:00', 31, 3,    1),
(98,  '2026-01-09 14:00:00', 'Keyboard sale',          'Online order',     'COMPLETED',   360.00,  4, 'SALE',              '2026-01-09 14:00:00', 22, NULL, 2),
(99,  '2026-01-14 11:00:00', 'Desk organizer sale',    'Office supply',    'COMPLETED',   200.00, 10, 'SALE',              '2026-01-14 11:00:00', 50, NULL, 2),
(100, '2026-01-18 09:00:00', 'Vase restock',           'Regular order',    'COMPLETED',   440.00, 20, 'PURCHASE',          '2026-01-18 17:00:00', 43, 7,    1),
(101, '2026-01-22 15:00:00', 'Rice cooker sale',       'Walk-in customer', 'COMPLETED',   225.00,  5, 'SALE',              '2026-01-22 15:00:00', 29, NULL, 2),
(102, '2026-01-28 10:00:00', 'Laptop cosmetic return', 'Cosmetic damage',  'PROCESSING',    0.00,  1, 'RETURN_TO_SUPPLIER','2026-01-28 10:00:00', 16, 1,    1),
(103, '2026-02-04 09:00:00', 'Headphones restock',     'Bulk purchase',    'COMPLETED',  1200.00, 15, 'PURCHASE',          '2026-02-04 17:00:00', 17, 8,    1),
(104, '2026-02-08 10:00:00', 'Sports shorts restock',  'New season stock', 'COMPLETED',   800.00, 40, 'PURCHASE',          '2026-02-08 18:00:00', 27, 5,    1),
(105, '2026-02-12 14:00:00', 'Headphones sale',        'Online order',     'COMPLETED',   480.00,  6, 'SALE',              '2026-02-12 14:00:00', 17, NULL, 2),
(106, '2026-02-17 11:00:00', 'Vase sale',              'Retail store',     'COMPLETED',   176.00,  8, 'SALE',              '2026-02-17 11:00:00', 43, NULL, 2),
(107, '2026-02-21 09:00:00', 'Picture frame restock',  'Regular order',    'COMPLETED',   480.00, 40, 'PURCHASE',          '2026-02-21 17:00:00', 15, 3,    1),
(108, '2026-02-26 16:00:00', 'Picture frame sale',     'Walk-in customer', 'COMPLETED',   180.00, 15, 'SALE',              '2026-02-26 16:00:00', 15, NULL, 2),
(109, '2026-03-03 09:00:00', 'Gaming mouse restock',   'Bulk purchase',    'COMPLETED',   700.00, 20, 'PURCHASE',          '2026-03-03 17:00:00', 48, 8,    1),
(110, '2026-03-07 14:00:00', 'Gaming mouse sale',      'Online order',     'COMPLETED',   280.00,  8, 'SALE',              '2026-03-07 14:00:00', 48, NULL, 2),
(111, '2026-03-11 09:00:00', 'Soccer ball restock',    'Sports season',    'COMPLETED',   375.00, 15, 'PURCHASE',          '2026-03-11 17:00:00', 42, 6,    1),
(112, '2026-03-15 11:00:00', 'Soccer ball sale',       'Retail store',     'COMPLETED',   150.00,  6, 'SALE',              '2026-03-15 11:00:00', 42, NULL, 2),
(113, '2026-03-19 15:00:00', 'Smartphone A2 sale',     'Corporate order',  'COMPLETED',  1200.00,  2, 'SALE',              '2026-03-19 15:00:00',  2, NULL, 2),
(114, '2026-03-25 09:00:00', 'Cookware restock',       'Monthly order',    'COMPLETED',  1200.00, 20, 'PURCHASE',          '2026-03-25 17:00:00',  7, 10,   1),
(115, '2026-04-02 14:00:00', 'Hoodie sale',            'Spring sale',      'COMPLETED',   300.00,  6, 'SALE',              '2026-04-02 14:00:00', 26, NULL, 2),
(116, '2026-04-07 11:00:00', 'Jump rope sale',         'Online order',     'COMPLETED',   120.00, 10, 'SALE',              '2026-04-07 11:00:00', 41, NULL, 2),
(117, '2026-04-10 09:00:00', 'Sunglasses spring restock','Spring stock',   'COMPLETED',  1050.00, 30, 'PURCHASE',          '2026-04-10 17:00:00', 32, 11,   1),
(118, '2026-04-14 15:00:00', 'Sunglasses sale',        'Spring sale',      'COMPLETED',   350.00, 10, 'SALE',              '2026-04-14 15:00:00', 32, NULL, 2),
(119, '2026-04-18 11:00:00', 'Cookware sale',          'Walk-in customer', 'COMPLETED',   240.00,  4, 'SALE',              '2026-04-18 11:00:00',  7, NULL, 2),
(120, '2026-04-22 09:00:00', 'Winter jacket advance stock','Advance winter','COMPLETED',  900.00, 20, 'PURCHASE',          '2026-04-22 17:00:00',  5, 2,    1);

SET FOREIGN_KEY_CHECKS = 1;

SET FOREIGN_KEY_CHECKS = 1;
