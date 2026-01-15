# Phase 7 Design Documentation

## Step 1: Discovery Summary

### REST Endpoints
- **POST /api/v1/orders/checkout** - Checkout cart and create order (with idempotency support)
- **GET /api/v1/orders/{orderId}** - Get order by ID (NOT IMPLEMENTED - throws exception)

### Async Entrypoints
- **None** - No Kafka listeners, @Async methods, or scheduled jobs in Phase 7 order module

### External Integrations
- **None** - Payment gateway integration deferred to Phase 9 (PaymentIntentService uses placeholder "INTERNAL" provider)

### Persistence Layer

#### JPA Entities
- `Order` → `orders` table
- `OrderItem` → `order_items` table
- `PaymentIntent` → `payment_intents` table
- `IdempotencyKey` → `idempotency_keys` table
- `InventoryReservation` → `inventory_reservations` table

#### Repositories
- `OrderRepository`
- `OrderItemRepository`
- `PaymentIntentRepository`
- `IdempotencyKeyRepository`
- `InventoryReservationRepository`

#### Database Migrations
- `V5__create_order_and_payment_tables.sql` - Creates all Phase 7 tables

### Business Flows Identified
1. **Checkout Flow** - Main flow for creating orders from carts
   - Happy Path: Successful checkout with idempotency
   - Failure Path 1: Cart validation failures (expired, empty, not found)
   - Failure Path 2: Inventory reservation failures (insufficient stock)
   - Failure Path 3: Idempotency key handling (duplicate request)

---

## Step 2: Sequence Diagrams

See separate PlantUML files:
- `Checkout_Happy.puml` - Successful checkout flow
- `Checkout_CartValidationFailure.puml` - Cart validation failures
- `Checkout_InventoryFailure.puml` - Inventory reservation failures
- `Checkout_Idempotency.puml` - Idempotency key handling

---

## Step 3: Database ERD

See `PHASE_7_DATABASE_ERD.puml` for complete database model.

---

## Step 4: Traceability Table

See traceability table below mapping flows to endpoints, services, and tables.
