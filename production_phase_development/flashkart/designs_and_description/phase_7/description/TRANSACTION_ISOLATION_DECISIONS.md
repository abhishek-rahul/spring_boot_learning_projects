# Phase 7: Transaction Isolation Level Decisions

## Overview

This document describes the transaction isolation level decisions made for Phase 7 (Checkout) implementation.

## Isolation Level: READ_COMMITTED

**Decision:** Use `READ_COMMITTED` isolation level for checkout transactions.

**Rationale:**
1. **Prevents Dirty Reads**: Ensures we don't read uncommitted data from other transactions
2. **Allows Concurrency**: Multiple checkout transactions can proceed concurrently without blocking each other unnecessarily
3. **Pessimistic Locking**: We use pessimistic locking (`PESSIMISTIC_WRITE`) on SKU entities during inventory reservation to prevent overselling
4. **Performance**: `READ_COMMITTED` provides better performance than `SERIALIZABLE` while still maintaining correctness with proper locking

## Transaction Boundaries

### Checkout Transaction (`CheckoutService.checkout()`)

**Isolation:** `READ_COMMITTED`  
**Scope:** Entire checkout operation (atomic)

**Operations within transaction:**
1. Cart validation
2. Order draft creation
3. Order items creation
4. Inventory reservation (with pessimistic locking)
5. Payment intent creation
6. Order status transition to PENDING_PAYMENT

**Rollback Rules:**
- `BusinessException` (non-retryable): Rollback transaction
- `BusinessException` (retryable): Rollback transaction (client should retry with same idempotency key)
- `RuntimeException`: Rollback transaction
- No rollback for: checked exceptions (if any)

### Inventory Reservation Transaction (`InventoryReservationService.reserveInventory()`)

**Isolation:** `READ_COMMITTED`  
**Locking:** Pessimistic write lock on SKU entities

**Why Pessimistic Locking:**
- Prevents concurrent modifications to stock quantities
- Ensures atomicity of stock reservation
- Prevents race conditions in high concurrency scenarios
- Trade-off: Slight performance impact for correctness guarantee

**Lock Scope:**
- Each SKU is locked individually during reservation
- Lock is held until transaction commits or rolls back
- Other transactions trying to reserve the same SKU will wait

## Concurrency Scenarios

### Scenario 1: Concurrent Checkouts for Different SKUs
- **Behavior:** Both transactions proceed concurrently
- **Isolation:** `READ_COMMITTED` allows this
- **Result:** No blocking, both succeed

### Scenario 2: Concurrent Checkouts for Same SKU
- **Behavior:** First transaction acquires lock, second waits
- **Isolation:** Pessimistic locking ensures sequential processing
- **Result:** First succeeds if stock available, second may fail if insufficient stock

### Scenario 3: Checkout + Cart Update
- **Behavior:** Checkout reads cart snapshot, cart updates don't affect checkout
- **Isolation:** `READ_COMMITTED` ensures consistent read
- **Result:** Checkout uses cart state at transaction start

## Alternative Isolation Levels Considered

### SERIALIZABLE
- **Pros:** Highest isolation, prevents all anomalies
- **Cons:** Poor performance, excessive locking, potential deadlocks
- **Decision:** Not chosen - overkill for our use case

### REPEATABLE_READ
- **Pros:** Prevents non-repeatable reads
- **Cons:** Not necessary for checkout flow (we read once)
- **Decision:** Not chosen - unnecessary overhead

### READ_UNCOMMITTED
- **Pros:** Best performance
- **Cons:** Allows dirty reads, unacceptable for financial transactions
- **Decision:** Not chosen - violates correctness requirements

## Monitoring and Observability

**Key Metrics to Monitor:**
1. Transaction duration (p95, p99)
2. Lock wait time
3. Rollback rate
4. Concurrent checkout attempts
5. Inventory reservation conflicts

**Alerts:**
- High lock wait times (> 1 second)
- High rollback rate (> 5%)
- Transaction timeouts

## Future Considerations

1. **Optimistic Locking**: Consider optimistic locking with version fields for Phase 8 if pessimistic locking becomes a bottleneck
2. **Distributed Locking**: For multi-instance deployments, consider Redis-based distributed locks
3. **Read Replicas**: Use read replicas for cart reads to reduce load on primary database
