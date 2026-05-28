# USER-SERVICE IMPLEMENTATION STATUS & DOCUMENTATION

## 1. Service Overview

### Module Name

user-service

### Port

8081

### Responsibility

Handles all Identity and Access Management (IAM) functions, including:

* User registration
* JWT-based authentication
* Refresh token workflow
* Profile management
* Password management
* Token blacklisting via Redis

---

# 2. Technologies Implemented

## Spring Boot 3.x

Purpose:

* Core backend framework
* Dependency injection
* REST API development
* Application configuration

---

## Spring Security & BCrypt

Purpose:

* Endpoint protection
* Authentication flow
* Password hashing

Features:

* BCrypt password encryption
* Security filter chain
* Stateless authentication

---

## JJWT (0.12.x)

Purpose:

* JWT generation
* JWT parsing
* JWT validation

Token Types:

* Access Token
* Refresh Token

---

## Spring Data Redis

Purpose:

* Redis integration
* JWT blacklist management
* Stateless token revocation

---

## Spring Data JPA & Hibernate

Purpose:

* ORM mapping
* Database communication
* Repository abstraction

Database:

* MySQL

---

## Global Exception Handler (@RestControllerAdvice)

Purpose:

* Centralized exception handling
* Structured JSON error responses

Supported Status Codes:

* 400 Bad Request
* 401 Unauthorized
* 404 Not Found

---

# 3. Completed Phases & Workflows

# Phase 1 — Foundation & Registration

## Entities & Repositories

Implemented:

* User entity
* UserRepository

Database Mapping:

* users table

Repository Methods:

* existsByPhone()
* findByPhone()

---

## Registration Workflow

### Endpoint

POST /api/users

### Responsibilities

* Accept registration DTO
* Validate unique phone number
* Hash password using BCrypt
* Save user to database
* Return sanitized response

### Security Notes

* Password never returned in response
* Validation before persistence

---

# Phase 2 — Core Authentication (Login & Refresh)

## JWT Token Generation

### Service

JwtService

### Features

* Generate Access Token
* Generate Refresh Token
* Parse token claims
* Validate token signature
* Validate expiration

---

## Access Token

### Contains

* userId
* role

### Purpose

* Authorization
* Access protected APIs

### Characteristics

* Short-lived

---

## Refresh Token

### Contains

* phone number as subject

### Purpose

* Generate new token pair

### Characteristics

* Long-lived

---

## Environment Security

Secrets are loaded from:

* .env file

Purpose:

* Avoid hardcoded secrets
* Improve deployment security

---

## Login Workflow

### Endpoint

POST /api/auth/login

### Flow

1. Validate phone number
2. Validate password
3. Generate token pair
4. Return Access + Refresh Tokens

---

## Refresh Workflow

### Endpoint

POST /api/auth/refresh

### Flow

1. Accept Refresh Token
2. Validate token
3. Extract subject
4. Validate user existence
5. Generate new token pair
6. Return updated tokens

---

# Phase 3 — Protected Resources & Profile Management

## Security Filter

### Component

JwtAuthenticationFilter

### Type

OncePerRequestFilter

### Responsibilities

* Extract Bearer token
* Validate JWT
* Validate expiration
* Extract userId
* Populate SecurityContextHolder

---

## Authentication Flow

### Request Processing

1. Read Authorization header
2. Extract Bearer token
3. Validate token signature
4. Validate token expiration
5. Authenticate user
6. Store authentication context

---

## Profile APIs

### Get Current User Profile

#### Endpoint

GET /api/users/me

#### Purpose

Return authenticated user profile

#### Authentication

Uses Principal from SecurityContext

---

### Update Profile

#### Endpoint

PUT /api/users/me/profile

#### Editable Fields

* fullName
* avatarUrl

---

# Phase 4 — Redis Token Blacklisting

# Problem Statement

JWT authentication is stateless.

Issue:

* JWT remains valid after logout
* JWT remains valid after password change

This creates security risks.

---

# Solution — Redis Blacklist

## Component

TokenBlacklistService

### Purpose

Invalidate active JWT tokens before expiration.

---

## Blacklist Workflow

### Logic

1. Token becomes invalid
2. Store token in Redis
3. Set TTL equal to remaining expiration time
4. Reject future requests using token

---

## Redis TTL Strategy

Purpose:

* Automatic cleanup
* Memory optimization
* No manual deletion required

---

## Updated JwtAuthenticationFilter

### Additional Validation

Before authenticating:

* Check Redis blacklist

If token exists:

* Immediately reject request
* Return 401 Unauthorized

---

## Logout API

### Endpoint

POST /api/auth/logout

### Flow

1. Read current Access Token
2. Calculate remaining lifetime
3. Add token to Redis blacklist
4. Invalidate current session

---

## Secure Password Change

### Endpoint

PUT /api/users/me/password

### Flow

1. Validate current password
2. Update database password
3. Extract current Access Token
4. Add token to Redis blacklist
5. Force user re-login

---

# 4. Security Architecture

## Authentication Type

Stateless JWT Authentication

---

## Security Layers

### Layer 1 — BCrypt

Protect stored passwords.

---

### Layer 2 — JWT Signature Validation

Prevent token tampering.

---

### Layer 3 — JWT Expiration Validation

Prevent long-term token abuse.

---

### Layer 4 — Redis Blacklist

Enable manual token revocation.

---

## Security Goals

* Prevent replay attacks
* Prevent stale token reuse
* Support secure logout
* Support secure password changes

---

# 5. Current API Summary

## Public APIs

### Register

POST /api/users

### Login

POST /api/auth/login

### Refresh Token

POST /api/auth/refresh

---

## Protected APIs

### Current User Profile

GET /api/users/me

### Update Profile

PUT /api/users/me/profile

### Logout

POST /api/auth/logout

### Change Password

PUT /api/users/me/password

---

# 6. Redis Integration Details

## Redis Usage

Purpose:

* JWT token blacklist

---

## Blacklist Key Strategy

Suggested Format:
blacklist:{token}

---

## Expiration Strategy

TTL:
Remaining JWT expiration time

Benefit:
Automatic cleanup

---

# 7. Database Integration

## ORM Framework

* Spring Data JPA
* Hibernate

---

## Current Entity

### User

Mapped Table:
users

---

## Core User Fields

* id
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

# 8. Exception Handling

## Global Exception Handler

### Annotation

@RestControllerAdvice

---

## Responsibilities

* Catch RuntimeExceptions
* Standardize JSON responses
* Return proper HTTP status codes

---

## Supported Error Types

### 400 Bad Request

Validation failures

### 401 Unauthorized

Authentication failures

### 404 Not Found

Missing resources

---

# 9. Pending Features (Next Steps)

# Phase 5 — Address Management

## Planned Entity

### Address

Relationship:

* User → Multiple Addresses

Mapped Table:
addresses

---

## Planned Features

### CRUD APIs

Operations:

* Create address
* Get addresses
* Update address
* Delete address

---

## Default Address Logic

Requirements:

* Only one default address per user
* Auto-update previous default

---

## Planned Fields

* id
* user_id
* address_line
* latitude
* longitude
* is_default

---

# 10. Suggested Future Improvements

## OTP Authentication

Possible Features:

* SMS OTP
* Email OTP
* Password reset verification

---

## Rate Limiting

Purpose:

* Prevent brute-force attacks

---

## Email Verification

Purpose:

* Validate user email ownership

---

## Device Session Management

Purpose:

* Track active sessions
* Remote logout support

---

## Role-Based Authorization

Purpose:

* Fine-grained endpoint permissions

---

# 11. Current System Status

## Completed

* Registration
* Login
* JWT Authentication
* Refresh Token Workflow
* Security Filter
* Profile APIs
* Redis Token Blacklist
* Logout Workflow
* Secure Password Change

---

## In Progress

* None

---

## Pending

* Address Management
* OTP Features
* Advanced Authorization
* Session Tracking

---

# 12. Engineering Notes

## Architecture Style

* Stateless authentication
* Layered architecture
* Service-oriented design

---

## Important Design Decisions

### Redis Blacklist Instead of Stateful Sessions

Reason:
Maintain scalability while supporting token revocation.

---

### Short-lived Access Tokens

Reason:
Reduce attack window if token leaks.

---

### Long-lived Refresh Tokens

Reason:
Improve user experience.

---

# 13. Final Objective

Build a secure, scalable, stateless authentication service with:

* JWT authentication
* Refresh token workflow
* Redis-based token revocation
* Secure password management
* Extensible IAM architecture
