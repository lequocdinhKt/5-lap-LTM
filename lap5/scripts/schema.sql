-- Chạy trên MySQL Server local độc lập của Máy A và Máy B.
-- Script chỉ tạo schema mới, không phải migration dữ liệu MariaDB cũ.
CREATE DATABASE IF NOT EXISTS lab5_warehouse
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE lab5_warehouse;

CREATE TABLE IF NOT EXISTS users (
    id CHAR(36) PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'STAFF',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS products (
    id CHAR(36) PRIMARY KEY,
    product_code VARCHAR(100) NOT NULL UNIQUE,
    product_name VARCHAR(255) NOT NULL,
    category VARCHAR(255) NOT NULL,
    price DECIMAL(18,2) NOT NULL CHECK (price >= 0),
    quantity INT NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    version BIGINT NOT NULL DEFAULT 0,
    last_fencing_token BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS product_locks (
    resource_id CHAR(36) PRIMARY KEY,
    owner_client_id VARCHAR(255) NOT NULL,
    lock_token CHAR(36) NOT NULL UNIQUE,
    fencing_token BIGINT NOT NULL,
    acquired_at DATETIME(3) NOT NULL,
    expires_at DATETIME(3) NOT NULL,
    FOREIGN KEY (resource_id) REFERENCES products(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS inventory_transactions (
    id CHAR(36) PRIMARY KEY,
    transaction_code VARCHAR(100) NOT NULL UNIQUE,
    product_id CHAR(36) NOT NULL,
    transaction_type VARCHAR(20) NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    quantity_before INT NOT NULL CHECK (quantity_before >= 0),
    quantity_after INT NOT NULL CHECK (quantity_after >= 0),
    created_by CHAR(36) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    FOREIGN KEY (product_id) REFERENCES products(id),
    FOREIGN KEY (created_by) REFERENCES users(id),
    INDEX history_product_time (product_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
