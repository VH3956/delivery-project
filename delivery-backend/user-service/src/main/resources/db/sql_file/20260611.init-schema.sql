-- 1. Create Roles Table (Extends BaseEntity)
CREATE TABLE IF NOT EXISTS roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP(6),
    updated_at TIMESTAMP(6),
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

-- 2. Create Users Table (Extends BaseEntity)
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(36) PRIMARY KEY,
    phone VARCHAR(15) NOT NULL UNIQUE,
    email VARCHAR(100) UNIQUE,        -- Matches: nullable = true, length = 100
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255),           -- Matches: nullable = true
    avatar_url VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP(6),
    updated_at TIMESTAMP(6),
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

-- 3. Create User_Roles Mapping Table (Extends BaseEntity)
CREATE TABLE IF NOT EXISTS user_roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    role_id BIGINT NOT NULL,
    created_at TIMESTAMP(6),
    updated_at TIMESTAMP(6),
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

-- 4. Create Shipper Profiles Table (Does NOT extend BaseEntity)
CREATE TABLE IF NOT EXISTS shipper_profiles (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL UNIQUE,
    identity_card_number VARCHAR(20) NOT NULL UNIQUE, -- Matches: length = 20
    driving_license VARCHAR(50) NOT NULL,
    vehicle_plate VARCHAR(20) NOT NULL,               -- Matches: length = 20
    rating DECIMAL(2, 1),                             -- Matches: precision = 2, scale = 1
    is_approved BOOLEAN NOT NULL DEFAULT FALSE,
    is_online BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_shipper_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 5. Create Addresses Table (Does NOT extend BaseEntity)
CREATE TABLE IF NOT EXISTS addresses (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    address_line VARCHAR(255) NOT NULL,
    latitude DECIMAL(10, 8),                          -- Matches: precision = 10, scale = 8
    longitude DECIMAL(11, 8),                         -- Matches: precision = 11, scale = 8
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_address_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 6. Create Shipper Reviews Table
CREATE TABLE IF NOT EXISTS shipper_reviews (
    id VARCHAR(36) PRIMARY KEY,
    shipper_id VARCHAR(36) NOT NULL,
    customer_id VARCHAR(36) NOT NULL,
    order_id VARCHAR(36) NOT NULL UNIQUE, -- One review per order
    rating INT NOT NULL,                  -- 1 to 5 stars
    comment TEXT,
    created_at TIMESTAMP(6),
    CONSTRAINT fk_review_shipper FOREIGN KEY (shipper_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_review_customer FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE CASCADE
);