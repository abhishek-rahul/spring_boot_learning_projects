# FlashSale Application - Complete Design Documentation

This directory contains comprehensive design documentation for the FlashSale Spring Boot application, generated from codebase analysis.

## Documentation Structure

### 1. Discovery Report (`01_discovery.md`)
Complete inventory of:
- All REST endpoints (11 endpoints across 3 controllers)
- Async operations (CompletableFuture, background workers)
- External integrations (simulated payment and email services)
- Persistence layer analysis (in-memory structures only)
- Security configuration (none found)
- Business flows identified

### 2. Sequence Diagrams (`02_sequence_diagrams.puml`)
PlantUML sequence diagrams for all major business flows:

#### Order Placement Flows
- `OrderPlacement_HappyPath` - Successful order with inventory reservation, payment, and email
- `OrderPlacement_OutOfStock` - Failure when inventory is insufficient
- `OrderPlacement_PaymentFailed` - Failure when payment simulation fails
- `OrderPlacement_MaintenanceMode` - Rejection when maintenance mode is active

#### Catalog & Inventory Flows
- `CatalogLookup_HappyPath` - Read catalog information with ReadWriteLock
- `InventoryCheck_HappyPath` - Check inventory stock levels

#### Admin Operations
- `AdminMaintenance_HappyPath` - Toggle maintenance mode on/off

#### Load Testing
- `LoadTest_HappyPath` - Setup barrier and execute simultaneous order placement

#### System Operations
- `StartupFlow_HappyPath` - Application initialization with CountDownLatch coordination
- `EmailQueue_HappyPath` - Background email processing via EventQueueWorker

#### Debug Operations
- `DebugDeadlock_HappyPath` - Demonstrate deadlock scenario
- `DebugDeadlockFix_HappyPath` - Demonstrate deadlock prevention with consistent lock ordering

### 3. Database ERD (`03_database_erd.puml`)
**IMPORTANT:** This application uses **in-memory data structures only** - no persistent database.

The ERD documents:
- Logical data model (as if persisted)
- In-memory structures:
  - CatalogService.cache (HashMap with ReadWriteLock)
  - InventoryService.stock (ConcurrentHashMap)
  - InventoryService.locks (ConcurrentHashMap of ReentrantLocks)
  - EventQueueWorker.queue (BlockingQueue)
  - Metrics (AtomicLong counters)
- Assumptions and missing persistence components

### 4. Traceability Table (`04_traceability_table.md`)
Complete mapping of:
- Business flows → REST endpoints
- Endpoints → Services involved
- Services → Data structures accessed
- Flows → Sequence diagrams

## Key Findings

### Architecture Highlights
1. **No Database Persistence**: All data is in-memory (ConcurrentHashMap, HashMap)
2. **Thread-Safe Design**: Uses ReentrantLock, ReadWriteLock, ConcurrentHashMap, AtomicLong
3. **Async Processing**: CompletableFuture with separate thread pools for CPU-bound and IO-bound tasks
4. **Background Workers**: EventQueueWorker with BlockingQueue for email processing
5. **No Security**: All endpoints are publicly accessible (no authentication)

### Thread Safety Mechanisms
- **InventoryService**: Per-SKU ReentrantLock (fair locks) for stock reservation
- **CatalogService**: ReadWriteLock for read-heavy catalog access
- **OrderService**: CompletableFuture with thread pool executors
- **EventQueueWorker**: BlockingQueue with worker thread pool

### Executor Configuration
- **orderExecutor**: ThreadPoolExecutor (core: 20, max: 50, queue: 500) for order processing
- **ioExecutor**: ThreadPoolExecutor (core: 10, max: 30, queue: 300) for payment/email simulation

### Assumptions Documented
1. No database persistence (data lost on restart)
2. No order entity (orders not tracked)
3. No user management (no authentication)
4. Payment is simulated (no real payment gateway)
5. Email is simulated (no real email service)

## How to Use This Documentation

### Viewing PlantUML Diagrams
1. Install PlantUML plugin in your IDE (VS Code, IntelliJ, etc.)
2. Open `.puml` files - diagrams will render automatically
3. Or use online PlantUML server: http://www.plantuml.com/plantuml/uml/

### Generating Images
```bash
# Using PlantUML CLI
plantuml designs/02_sequence_diagrams.puml
plantuml designs/03_database_erd.puml
```

### Understanding the Flows
1. Start with `01_discovery.md` for overview
2. Review `04_traceability_table.md` to find relevant flows
3. Open corresponding sequence diagrams in `02_sequence_diagrams.puml`
4. Reference `03_database_erd.puml` for data structure relationships

## File Summary

| File | Description | Lines |
|------|-------------|-------|
| `01_discovery.md` | Complete codebase inventory | ~100 |
| `02_sequence_diagrams.puml` | 11 sequence diagrams | ~600 |
| `03_database_erd.puml` | ERD + assumptions | ~150 |
| `04_traceability_table.md` | Flow → Endpoint → Service mapping | ~250 |

## Next Steps (Recommendations)

If this were a production system, consider:
1. **Add Database Persistence**
   - JPA entities for Order, Inventory, Payment, EmailLog
   - Spring Data repositories
   - Database migrations (Flyway/Liquibase)

2. **Add Security**
   - Spring Security configuration
   - JWT authentication
   - Role-based access control (admin endpoints)

3. **Add Observability**
   - Distributed tracing (Micrometer, Zipkin)
   - Structured logging
   - Health checks

4. **Add Resilience**
   - Circuit breakers for external services
   - Retry mechanisms
   - Timeout handling

5. **Add Transaction Management**
   - @Transactional for order placement
   - Rollback on payment failure
   - Saga pattern for distributed transactions

---

**Generated:** 2025-01-15  
**Codebase Analyzed:** FlashSale Spring Boot Application  
**Spring Boot Version:** 4.0.1  
**Java Version:** 21
