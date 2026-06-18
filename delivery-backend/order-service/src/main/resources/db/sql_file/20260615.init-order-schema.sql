-- 1. Create Vouchers Table
CREATE TABLE vouchers (
    id VARCHAR(36) PRIMARY KEY,
    code VARCHAR(255) NOT NULL UNIQUE,
    discount_amount DECIMAL(10, 2) NOT NULL,
    min_order_value DECIMAL(10, 2) NOT NULL,
    is_active BOOLEAN NOT NULL
);

-- 2. Create Orders Table
CREATE TABLE orders (
    id VARCHAR(36) PRIMARY KEY,
    customer_id VARCHAR(255) NOT NULL,
    shipper_id VARCHAR(255),
    pickup_address_id VARCHAR(255),
    delivery_address_id VARCHAR(255),
    pickup_lat DOUBLE,
    pickup_lng DOUBLE,
    pickup_address_line VARCHAR(255),
    delivery_lat DOUBLE,
    delivery_lng DOUBLE,
    delivery_address_line VARCHAR(255),
    voucher_id VARCHAR(255),
    
    -- FIXED: Use MySQL ENUM instead of VARCHAR
    status ENUM('CREATED', 'PAID', 'ASSIGNED', 'PICKED_UP', 'IN_TRANSIT', 'DELIVERED', 'COMPLETED', 'CANCELLED') NOT NULL,
    
    item_name VARCHAR(255) NOT NULL,
    item_weight DECIMAL(5, 2),
    note TEXT,
    distance_km DECIMAL(6, 2),
    delivery_fee DECIMAL(10, 2) NOT NULL,
    cod_amount DECIMAL(10, 2),
    total_amount DECIMAL(10, 2),
    delivery_photo_url VARCHAR(255),
    cancel_reason TEXT,
    created_at TIMESTAMP(6),
    updated_at TIMESTAMP(6)
);

-- 3. Create Order Timelines Table
CREATE TABLE order_timelines (
    id VARCHAR(36) PRIMARY KEY,
    order_id VARCHAR(36) NOT NULL,
    
    -- FIXED: Use MySQL ENUM instead of VARCHAR
    status ENUM('CREATED', 'PAID', 'ASSIGNED', 'PICKED_UP', 'IN_TRANSIT', 'DELIVERED', 'COMPLETED', 'CANCELLED') NOT NULL,
    
    description TEXT NOT NULL,
    created_at TIMESTAMP(6),
    CONSTRAINT fk_timeline_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);