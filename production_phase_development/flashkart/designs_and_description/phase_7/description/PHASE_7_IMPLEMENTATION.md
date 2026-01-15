# Phase 7 Implementation: Checkout (Transactions + Idempotency)

## Overview

This PR implements Phase 7 of the FlashKart e-commerce platform, focusing on **Checkout with Transactions and Idempotency**. The implementation provides a production-grade checkout flow with atomic transactions, inventory reservation, payment intent creation, and idempotency support for safe retries.

## Goals Achieved ✅

1. **POST /checkout atomic flow** - Complete checkout orchestration
2. **Order draft create** - Order entity with state machine
3. **Inventory reservation** - Stock reservation with pessimistic locking
4. **Payment intent** - Payment intent creation for orders
5. **Idempotency keys** - Safe retry mechanism for checkout requests
6. **Transaction boundaries** - Proper transaction management with rollback rules
7. **Isolation level decisions** - Documented and applied (READ_COMMITTED)

## Architecture & Design Principles

### Clean Architecture
- **Domain Layer**: Pure business entities (Order, OrderItem, PaymentIntent, InventoryReservation, IdempotencyKey)
- **Infrastructure Layer**: Repository interfaces and implementations
- **Service Layer**: Business logic and orchestration (CheckoutService, InventoryReservationService, PaymentIntentService, IdempotencyService)
- **API Layer**: Controllers and DTOs

### SOLID Principles Applied

1. **Single Responsibility**: Each service has one clear purpose
   - `CheckoutService`: Orchestrates checkout flow
   - `InventoryReservationService`: Manages inventory reservations
   - `PaymentIntentService`: Creates payment intents
   - `IdempotencyService`: Handles idempotency key management

2. **Open/Closed**: Extensible through interfaces
   - Repository interfaces allow extension without modification
   - Service interfaces can be extended for future enhancements

3. **Liskov Substitution**: Repository interfaces follow Spring Data contracts

4. **Interface Segregation**: Focused DTOs for different use cases
   - `CheckoutRequest`: Checkout input
   - `OrderResponse`: Order output
   - `PaymentIntentResponse`: Payment intent output

5. **Dependency Inversion**: Services depend on repository interfaces, not implementations

## Key Components

### Domain Entities

#### Order Entity
- **Purpose**: Represents a customer order
- **Features**:
  - State machine with valid transitions (DRAFT → PENDING_PAYMENT → PAID → PROCESSING → SHIPPED → DELIVERED)
  - Order number generation (ORD-YYYYMMDD-HHMMSS-UUID)
  - Total amount calculation
  - Cart reference

#### OrderItem Entity
- **Purpose**: Represents line items in an order
- **Features**:
  - SKU reference
  - Quantity and pricing
  - Total price calculation

#### PaymentIntent Entity
- **Purpose**: Represents payment attempts for orders
- **Features**:
  - Status tracking (CREATED → PENDING → SUCCEEDED/FAILED)
  - Payment provider integration ready
  - Failure reason tracking

#### InventoryReservation Entity
- **Purpose**: Tracks reserved stock during checkout
- **Features**:
  - TTL-based expiration (15 minutes)
  - Status tracking (RESERVED → CONFIRMED/RELEASED/EXPIRED)
  - Automatic stock release on failure

#### IdempotencyKey Entity
- **Purpose**: Ensures safe retry of checkout requests
- **Features**:
  - SHA-256 hashed keys for security
  - Response caching
  - TTL-based expiration (24 hours)

### Services

#### CheckoutService
- **Orchestrates** the entire checkout flow atomically
- **Transaction Isolation**: READ_COMMITTED
- **Operations**:
  1. Cart validation
  2. Order draft creation
  3. Order items creation
  4. Inventory reservation
  5. Payment intent creation
  6. Order status transition

#### InventoryReservationService
- **Manages** stock reservations with pessimistic locking
- **Features**:
  - Prevents overselling
  - Atomic stock reservation
  - Automatic release on failure
  - Confirmation on payment success

#### PaymentIntentService
- **Creates** payment intents for orders
- **Ready for** Phase 9 payment gateway integration

#### IdempotencyService
- **Handles** idempotency key validation and storage
- **Features**:
  - SHA-256 key hashing
  - Response caching
  - Duplicate request detection

### Transaction Management

#### Isolation Level: READ_COMMITTED
- **Rationale**: Prevents dirty reads while allowing concurrency
- **Locking**: Pessimistic write locks on SKU entities during reservation
- **Performance**: Balanced between correctness and performance

#### Rollback Rules
- `BusinessException` (non-retryable): Rollback
- `BusinessException` (retryable): Rollback (client retries with idempotency key)
- `RuntimeException`: Rollback
- Checked exceptions: No rollback (if any)

### API Endpoints

#### POST /api/v1/orders/checkout
- **Purpose**: Checkout a cart and create an order
- **Request**: `CheckoutRequest` (cartId, optional idempotencyKey)
- **Response**: `OrderResponse` with order details and payment intent
- **Idempotency**: Supports idempotency keys for safe retry
- **Status Codes**:
  - 201 Created: Checkout successful
  - 400 Bad Request: Validation error, insufficient stock, etc.
  - 409 Conflict: Cart expired, invalid state
  - 500 Internal Server Error: System error

## Database Schema

### New Tables

1. **orders** - Order master table
2. **order_items** - Order line items
3. **payment_intents** - Payment intent tracking
4. **inventory_reservations** - Stock reservation tracking
5. **idempotency_keys** - Idempotency key storage

### Migration
- `V5__create_order_and_payment_tables.sql` - Creates all order and payment related tables

## Error Handling

### New Error Codes
- `ORDER_NOT_FOUND`
- `ORDER_ALREADY_EXISTS`
- `ORDER_INVALID_STATE`
- `CHECKOUT_FAILED`
- `PAYMENT_INTENT_NOT_FOUND`
- `PAYMENT_INTENT_FAILED`
- `IDEMPOTENCY_KEY_INVALID`

## Testing Considerations

### Unit Tests (Future)
- CheckoutService transaction rollback scenarios
- InventoryReservationService concurrency tests
- IdempotencyService duplicate request handling
- Order state machine transitions

### Integration Tests (Future)
- End-to-end checkout flow
- Concurrent checkout scenarios
- Idempotency key validation
- Inventory reservation conflicts

## Performance Considerations

1. **Pessimistic Locking**: May cause lock contention in high concurrency
   - **Mitigation**: Lock only during reservation, release quickly
   - **Future**: Consider optimistic locking in Phase 8

2. **Idempotency Key Lookup**: SHA-256 hash index for fast lookups

3. **Inventory Reservation Cleanup**: Scheduled job needed for expired reservations (Phase 8)

## Security Considerations

1. **Idempotency Keys**: SHA-256 hashed for security
2. **User Validation**: Cart ownership validation
3. **Stock Validation**: Prevents overselling
4. **Transaction Isolation**: Prevents race conditions

## Documentation

- `TRANSACTION_ISOLATION_DECISIONS.md` - Detailed isolation level decisions and rationale

## Future Enhancements (Phase 8+)

1. **Optimistic Locking**: For inventory management
2. **Distributed Locking**: For multi-instance deployments
3. **Inventory Reconciliation**: For leaked reservations
4. **Payment Gateway Integration**: Actual payment processing (Phase 9)
5. **Order Status Webhooks**: Event-driven order updates

## Breaking Changes

None - This is a new feature addition.

## Migration Guide

1. Run database migration: `V5__create_order_and_payment_tables.sql`
2. No code changes required for existing modules
3. New API endpoint available: `POST /api/v1/orders/checkout`

## Checklist

- [x] Database migration created
- [x] Domain entities implemented
- [x] Repositories implemented
- [x] Services implemented
- [x] API endpoints implemented
- [x] DTOs created
- [x] Error codes added
- [x] Transaction isolation documented
- [x] Code compiles successfully
- [x] Follows SOLID principles
- [x] Follows Clean Architecture
- [x] Proper error handling
- [x] Idempotency support
- [x] Inventory reservation with locking
