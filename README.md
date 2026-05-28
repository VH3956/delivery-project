# PROJECT MASTER CONTEXT — DELIVERY MANAGEMENT SYSTEM

## 1. Project Overview

### Project Name

Hệ thống quản lý giao hàng (On-Demand Delivery Management System)

### Goal

Build a real-time delivery ecosystem connecting:

* Customers (order creation, tracking)
* Shippers (accepting orders, updating status)
* Admins (system management, manual dispatch)

### Architecture

Microservices Architecture

### Primary Stack

#### Backend

* Java Spring Boot
* Multi-module Maven project

#### Frontend

* Vanilla JavaScript
* Vite

#### Infrastructure

* Docker
* Docker Compose

Purpose:
Containerize the entire application for one-command deployment.

---

# 2. Required Technology Stack & Purpose

## Docker / Docker Compose

Purpose:

* Containerize backend services
* Containerize frontend
* Containerize infrastructure services
* Simplify deployment

Services:

* MySQL
* Redis
* Kafka
* Zookeeper
* Backend Services
* Frontend

---

## Kafka

Purpose:

* Handle high concurrency
* Async communication between services
* Event-driven architecture

Use Cases:

* New order events
* Notification events
* Order timeline events
* Re-assigning shippers
* Payment events

---

## Redis

Purpose:

1. JWT Blacklist management
2. Store short-lived OTPs
3. Redis Geo for shipper location tracking

Features:

* Fast nearest-driver lookup
* Radius search (3-5km)
* Real-time shipper availability

---

## WebSocket

Purpose:

* Real-time communication

Use Cases:

* Push "New Order" alerts to Shippers
* Live GPS tracking for Customers
* Real-time order status updates

---

## Netflix Eureka

Purpose:

* Service discovery
* Dynamic microservice registration

---

# 3. Microservices Architecture

The backend is structured as a Maven Multi-Module project.

---

## Services

### eureka-server

Purpose:

* Service Registry

Responsibilities:

* Register microservices
* Service discovery

---

### api-gateway

Purpose:

* Single entry point

Responsibilities:

* Route requests
* Authentication filtering
* Rate limiting
* Request forwarding

---

### user-service

Responsibilities:

* Authentication
* JWT generation
* Refresh token management
* User profile management
* Address management
* Role management

Roles:

* ADMIN
* CUSTOMER
* SHIPPER

---

### order-service

Responsibilities:

* Order creation
* Order lifecycle management
* Timeline tracking
* Distance-based pricing
* Voucher handling

---

### delivery-service

Responsibilities:

* Core matching engine
* Find nearby shippers
* Assignment logic
* Re-assignment logic

Behavior:

* Listen to Kafka events
* Query Redis Geo
* Push assignments via WebSocket

---

### notification-service

Responsibilities:

* Send Email notifications
* Send SMS notifications

Behavior:

* Consume Kafka events

---

### payment-service

Responsibilities:

* COD deductions
* Wallet management
* Transaction history
* Balance updates

---

# 4. Key Functional Requirements

## Authentication & Users

Features:

* Register
* Login
* JWT Authentication
* Refresh Token
* Logout
* Token Blacklist
* Forgot Password
* OTP Verification
* Update Profile
* Manage Addresses
* Role Management

---

## Shipper Management

Features:

* Submit profile
* Upload CMND/CCCD
* Upload GPLX
* Admin approval workflow
* Online/Offline toggle

---

## Order Lifecycle

Flow:

1. Create order
2. Auto-assign or Manual-assign
3. Shipper accepts
4. Picked up
5. In transit
6. Delivered
7. Completed or Cancelled

---

## Concurrency & Re-assign

Requirements:

* Handle concurrent orders safely
* Prevent race conditions
* Auto re-assign rejected orders
* Kafka-based retry workflow

---

## Financials

Features:

* Distance fee calculation
* Google Maps API integration
* Voucher support
* COD management
* Wallet balance deduction

---

# 5. Database Design

## ENUMS

### role_enum

Possible Values:

* ADMIN
* CUSTOMER
* SHIPPER

---

### order_status_enum

Possible Values:

* CREATED
* ASSIGNED
* PICKED_UP
* IN_TRANSIT
* DELIVERED
* COMPLETED
* CANCELLED

---

### transaction_type_enum

Possible Values:

* DEPOSIT
* WITHDRAW
* COD_DEDUCTION
* FEE_PAYMENT
* REFUND

---

# 6. Database Tables

## users

Purpose:
Store system users.

Fields:

* id (UUID, PK)
* phone
* email
* password_hash
* full_name
* avatar_url
* role
* is_active
* created_at
* updated_at

---

## addresses

Purpose:
Store user addresses.

Fields:

* id
* user_id
* address_line
* latitude
* longitude
* is_default

---

## shipper_profiles

Purpose:
Store shipper verification information.

Fields:

* id
* user_id
* identity_card_number
* driving_license
* vehicle_plate
* rating
* is_approved
* is_online

---

## vouchers

Purpose:
Store discount vouchers.

Fields:

* id
* code
* discount_amount
* min_order_value
* valid_from
* valid_to
* is_active

---

## wallets

Purpose:
Store wallet balances.

Fields:

* id
* user_id
* balance
* updated_at

---

## transactions

Purpose:
Store wallet transaction history.

Fields:

* id
* wallet_id
* order_id
* amount
* type
* description
* created_at

---

## orders

Purpose:
Store delivery orders.

Fields:

* id
* customer_id
* shipper_id
* pickup_address_id
* delivery_address_id
* voucher_id
* status
* item_name
* item_weight
* note
* distance_km
* delivery_fee
* cod_amount
* total_amount
* delivery_photo_url
* cancel_reason
* created_at
* updated_at

---

## order_timelines

Purpose:
Store order status history.

Fields:

* id
* order_id
* status
* description
* created_at

---

## reviews

Purpose:
Store customer reviews for shippers.

Fields:

* id
* order_id
* customer_id
* shipper_id
* rating
* comment
* created_at

---

# 7. Core Business Flow

## Customer Creates Order

Flow:

1. Customer submits order
2. order-service validates data
3. Calculate distance and fee
4. Save order
5. Publish Kafka event

---

## Delivery Assignment

Flow:

1. delivery-service listens to Kafka
2. Query Redis Geo nearby shippers
3. Push WebSocket notification
4. First accepted shipper gets assignment
5. Update order status

---

## Order Tracking

Flow:

1. Shipper updates status
2. Publish Kafka event
3. Save timeline
4. Push WebSocket update to Customer

---

## Payment Flow

Flow:

1. Order completed
2. COD deduction
3. Wallet update
4. Save transaction history

---

# 8. Infrastructure Components

## MySQL

Purpose:
Primary relational database

---

## Redis

Purpose:
Caching and geolocation

---

## Kafka

Purpose:
Async messaging system

---

## Zookeeper

Purpose:
Kafka coordination

---

# 9. Important Engineering Constraints

## Backend

Rules:

* Use clean architecture
* Keep services independent
* Avoid direct service-to-service DB access
* Use DTOs for API communication
* Use Kafka for async communication

---

## Frontend

Rules:

* Keep UI lightweight
* Use modular components
* Real-time updates via WebSocket

---

## Infrastructure

Rules:

* Everything must run via Docker Compose
* One-command startup
* Environment variables centralized

---

# 10. Important System Behaviors

## Re-assignment Logic

Requirements:

* Detect rejected/cancelled orders
* Retry assignment automatically
* Prevent duplicate assignment

---

## Concurrency Safety

Requirements:

* Prevent multiple shippers accepting same order
* Ensure transaction consistency
* Use distributed locking if necessary

---

## Real-time Tracking

Requirements:

* Continuous shipper GPS updates
* Live customer tracking
* Low-latency communication

---

# 11. Suggested Development Priority

## Phase 1

* Eureka Server
* API Gateway
* User Service
* JWT Authentication

---

## Phase 2

* Order Service
* MySQL Integration
* Redis Integration

---

## Phase 3

* Kafka Integration
* Delivery Matching Engine

---

## Phase 4

* WebSocket Tracking
* Notification Service

---

## Phase 5

* Payment Service
* Wallet Management
* Voucher System

---

# 12. Final Objective

Build a scalable, production-ready, real-time delivery management platform using:

* Spring Boot Microservices
* Kafka
* Redis Geo
* WebSocket
* Docker Infrastructure
* Event-driven architecture
