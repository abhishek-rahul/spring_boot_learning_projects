# Phase 7 Traceability Table

## Mapping: Sequence Diagrams → Endpoints → Services → Tables

| Sequence Diagram | REST Endpoint | HTTP Method | Services Involved | Repositories Involved | Tables Touched | Transaction Boundaries |
|-----------------|---------------|-------------|-------------------|----------------------|----------------|------------------------|
| **Checkout_Happy** | `/api/v1/orders/checkout` | POST | `OrderController`<br>`IdempotencyService`<br>`CheckoutService`<br>`InventoryReservationService`<br>`PaymentIntentService`<br>`OrderNumberGenerator` | `IdempotencyKeyRepository`<br>`CartRepository`<br>`CartItemRepository`<br>`SkuRepository`<br>`OrderRepository`<br>`OrderItemRepository`<br>`InventoryReservationRepository`<br>`PaymentIntentRepository` | `idempotency_keys`<br>`carts`<br>`cart_items`<br>`skus`<br>`orders`<br>`order_items`<br>`inventory_reservations`<br>`payment_intents` | `@Transactional(READ_COMMITTED)`<br>Main transaction in `CheckoutService.checkout()`<br>Nested transaction in `InventoryReservationService.reserveInventory()` |
| **Checkout_CartValidationFailure** | `/api/v1/orders/checkout` | POST | `OrderController`<br>`IdempotencyService`<br>`CheckoutService` | `IdempotencyKeyRepository`<br>`CartRepository`<br>`CartItemRepository` | `idempotency_keys`<br>`carts`<br>`cart_items` | Transaction rolled back on validation failure |
| **Checkout_InventoryFailure** | `/api/v1/orders/checkout` | POST | `OrderController`<br>`IdempotencyService`<br>`CheckoutService`<br>`InventoryReservationService` | `IdempotencyKeyRepository`<br>`OrderRepository`<br>`OrderItemRepository`<br>`SkuRepository`<br>`InventoryReservationRepository` | `idempotency_keys`<br>`orders`<br>`order_items`<br>`skus`<br>`inventory_reservations` | Transaction rolled back on insufficient stock |
| **Checkout_Idempotency** | `/api/v1/orders/checkout` | POST | `OrderController`<br>`IdempotencyService`<br>`CheckoutService` | `IdempotencyKeyRepository`<br>`CartRepository`<br>`CartItemRepository`<br>`SkuRepository`<br>`OrderRepository`<br>`OrderItemRepository`<br>`InventoryReservationRepository`<br>`PaymentIntentRepository` | `idempotency_keys`<br>`carts`<br>`cart_items`<br>`skus`<br>`orders`<br>`order_items`<br>`inventory_reservations`<br>`payment_intents` | Idempotency check happens before main transaction |

## Code Classes → Tables Mapping

### OrderController
- **Touches Tables**: `idempotency_keys` (via IdempotencyService)
- **Read Operations**: None directly
- **Write Operations**: None directly (delegates to services)

### CheckoutService
- **Touches Tables**: 
  - `carts` (read)
  - `cart_items` (read)
  - `skus` (read)
  - `orders` (write)
  - `order_items` (write)
- **Transaction**: `@Transactional(READ_COMMITTED)` with rollback on `BusinessException` and `RuntimeException`

### InventoryReservationService
- **Touches Tables**:
  - `skus` (read with PESSIMISTIC_WRITE lock, write)
  - `inventory_reservations` (write)
- **Transaction**: `@Transactional(READ_COMMITTED)` nested within CheckoutService transaction

### PaymentIntentService
- **Touches Tables**:
  - `payment_intents` (write)
- **Transaction**: `@Transactional` nested within CheckoutService transaction

### IdempotencyService
- **Touches Tables**:
  - `idempotency_keys` (read, write)
- **Transaction**: 
  - `@Transactional(readOnly = true)` for lookups
  - `@Transactional` for storage (non-blocking - failures don't fail request)

### OrderRepository
- **Touches Tables**: `orders`
- **Operations**: 
  - `findById()`, `save()`
  - `findByOrderNumber()`
  - `findByIdAndUserId()`
  - `findByUserIdAndStatus()`

### OrderItemRepository
- **Touches Tables**: `order_items`
- **Operations**:
  - `save()`
  - `findByOrderId()`

### PaymentIntentRepository
- **Touches Tables**: `payment_intents`
- **Operations**:
  - `save()`
  - `findByOrderId()`

### IdempotencyKeyRepository
- **Touches Tables**: `idempotency_keys`
- **Operations**:
  - `save()`
  - `findByKeyHash()`
  - `deleteExpiredKeys()`

### InventoryReservationRepository
- **Touches Tables**: `inventory_reservations`
- **Operations**:
  - `save()`
  - `findByOrderId()`
  - `findBySkuIdAndStatus()`
  - `findExpiredReservations()`
  - `updateStatusByOrderId()`

## Transaction Isolation Levels

| Service Method | Isolation Level | Locking Strategy | Rationale |
|----------------|-----------------|-----------------|-----------|
| `CheckoutService.checkout()` | READ_COMMITTED | None (relies on nested services) | Prevents dirty reads, allows concurrency |
| `InventoryReservationService.reserveInventory()` | READ_COMMITTED | PESSIMISTIC_WRITE on SKU | Prevents overselling in high concurrency |
| `InventoryReservationService.confirmReservations()` | READ_COMMITTED | None | Bulk status update |
| `InventoryReservationService.releaseReservations()` | READ_COMMITTED | None | Releases stock on failure/cancellation |
| `PaymentIntentService.createPaymentIntent()` | Default (READ_COMMITTED) | None | Simple insert operation |
| `IdempotencyService.getExistingResponse()` | READ_COMMITTED (read-only) | None | Fast lookup for duplicate detection |
| `IdempotencyService.storeResponse()` | Default (READ_COMMITTED) | None | Non-critical operation (failures don't block request) |

## Assumptions and Notes

1. **Payment Gateway Integration**: Deferred to Phase 9. `PaymentIntentService` currently creates records with status "CREATED" and provider "INTERNAL" as placeholders.

2. **Order Retrieval**: `GET /api/v1/orders/{orderId}` endpoint exists but is not implemented (throws exception). Implementation deferred to Phase 8+.

3. **Kafka/Events**: No event publishing in Phase 7. Event-driven architecture will be added in Phase 10.

4. **Scheduled Jobs**: No scheduled jobs in Phase 7. Inventory reservation cleanup and idempotency key expiration cleanup can be added in future phases.

5. **External Integrations**: No external service calls (payment gateway, email, SMS) in Phase 7. These will be added in Phase 9+.

6. **Cart Deletion**: Cart is not deleted after checkout. Cart reference is stored in order but cart remains active (allows reordering).

7. **Inventory Reservation TTL**: Reservations expire after 15 minutes if not confirmed. Expired reservations should be cleaned up by a scheduled job (not implemented in Phase 7).

8. **Idempotency Key Expiry**: Idempotency keys expire after 24 hours. Cleanup method exists in repository but no scheduled job to call it.
