# Codebase Discovery Report

## REST Endpoints

### ApiController (`/api`)
- `POST /api/orders/place` - Place order (params: sku, qty)
- `GET /api/catalog/{sku}` - Get catalog item info
- `GET /api/inventory/{sku}` - Get inventory stock
- `POST /api/admin/maintenance` - Set maintenance mode (param: on)
- `GET /api/admin/maintenance` - Get maintenance mode state
- `GET /api/startup/remaining` - Get remaining startup steps
- `GET /api/metrics` - Get metrics (ordersPlaced, outOfStock, paymentsFailed, emailsQueued, emailQueueDropped)

### DebugController (`/debug`)
- `POST /debug/deadlock` - Trigger deadlock scenario
- `POST /debug/deadlock/fix` - Demonstrate deadlock fix

### LoadTestController (`/loadtest`)
- `POST /loadtest/setup` - Setup load test barrier (param: users)
- `POST /loadtest/run` - Run load test (params: users, sku, qty)

## Async Entrypoints

### CompletableFuture-based
- `OrderService.placeOrder()` - Returns CompletableFuture<String>
  - Uses `orderExecutor` for inventory reservation
  - Uses `ioExecutor` for payment simulation

### Background Workers
- `EventQueueWorker` - Processes email sending tasks asynchronously
  - Uses BlockingQueue (capacity: 1000)
  - Worker pool: 2 threads
  - Metrics tracked: emailsQueued, emailQueueDropped

## External Integrations

### Simulated Services
- **Payment Service**: `OrderService.paySimulated()` - Simulates 80ms payment processing
- **Email Service**: `OrderService.sendEmailSimulated()` - Simulates 200ms email sending (queued via EventQueueWorker)

## Persistence Layer

### ASSUMPTION: No Database
- **No JPA entities found**
- **No repositories found**
- **No database migrations found**
- **No database dependencies in pom.xml**

### In-Memory Data Structures
- `CatalogService`: HashMap<String, String> (SKU -> description)
- `InventoryService`: ConcurrentHashMap<String, Integer> (SKU -> stock quantity)
- `InventoryService`: ConcurrentHashMap<String, ReentrantLock> (SKU -> lock for thread safety)
- `Metrics`: AtomicLong counters (in-memory metrics)

## Security

### ASSUMPTION: No Security Configuration
- No Spring Security dependencies found
- No @PreAuthorize or security annotations
- All endpoints are publicly accessible (no authentication required)

## Configuration

### Application Properties
- `server.port=8081`
- `spring.application.name=flashsale`
- `management.endpoints.web.exposure.include=health,info`

### Executor Configuration
- `orderExecutor`: ThreadPoolExecutor (core: 20, max: 50, queue: 500)
- `ioExecutor`: ThreadPoolExecutor (core: 10, max: 30, queue: 300)

## Business Flows Identified

1. **Order Placement Flow** (Primary)
   - Happy path: Reserve inventory → Process payment → Queue email
   - Failure paths: Out of stock, Payment failed, Maintenance mode

2. **Catalog Lookup Flow**
   - Read catalog info with ReadWriteLock

3. **Inventory Check Flow**
   - Read inventory stock (thread-safe)

4. **Admin Operations Flow**
   - Maintenance mode toggle

5. **Load Testing Flow**
   - Setup barrier → Simultaneous order placement

6. **Debug Operations Flow**
   - Deadlock demonstration and fix

7. **Startup Flow**
   - CountDownLatch coordination (db, cache, warmup)

8. **Metrics Collection Flow**
   - Read atomic counters
