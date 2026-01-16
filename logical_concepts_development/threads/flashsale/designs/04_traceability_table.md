# Traceability Table: Flows → Endpoints → Services → Data Structures

## Format
**Flow Name** | **Endpoints** | **Services** | **Data Structures** | **Sequence Diagram**

---

## 1. Order Placement Flow

**Flow Name:** Order Placement (Happy Path)  
**Endpoints:** `POST /api/orders/place`  
**Services:** 
- ApiController
- StartupGate
- OrderService
- InventoryService
- EventQueueWorker
- Metrics

**Data Structures:**
- `InventoryService.stock` (ConcurrentHashMap<String, Integer>)
- `InventoryService.locks` (ConcurrentHashMap<String, ReentrantLock>)
- `EventQueueWorker.queue` (BlockingQueue<Runnable>)
- `Metrics.ordersPlaced` (AtomicLong)
- `Metrics.emailsQueued` (AtomicLong)

**Sequence Diagram:** `OrderPlacement_HappyPath`

---

**Flow Name:** Order Placement (Out of Stock)  
**Endpoints:** `POST /api/orders/place`  
**Services:**
- ApiController
- OrderService
- InventoryService
- Metrics

**Data Structures:**
- `InventoryService.stock` (ConcurrentHashMap<String, Integer>)
- `InventoryService.locks` (ConcurrentHashMap<String, ReentrantLock>)
- `Metrics.outOfStock` (AtomicLong)

**Sequence Diagram:** `OrderPlacement_OutOfStock`

---

**Flow Name:** Order Placement (Payment Failed)  
**Endpoints:** `POST /api/orders/place`  
**Services:**
- ApiController
- OrderService
- InventoryService
- Metrics

**Data Structures:**
- `InventoryService.stock` (ConcurrentHashMap<String, Integer>)
- `InventoryService.locks` (ConcurrentHashMap<String, ReentrantLock>)
- `Metrics.paymentsFailed` (AtomicLong)

**Sequence Diagram:** `OrderPlacement_PaymentFailed`

---

**Flow Name:** Order Placement (Maintenance Mode)  
**Endpoints:** `POST /api/orders/place`  
**Services:**
- ApiController
- OrderService

**Data Structures:**
- `OrderService.maintenanceMode` (volatile boolean)

**Sequence Diagram:** `OrderPlacement_MaintenanceMode`

---

## 2. Catalog Lookup Flow

**Flow Name:** Catalog Lookup  
**Endpoints:** `GET /api/catalog/{sku}`  
**Services:**
- ApiController
- StartupGate
- CatalogService

**Data Structures:**
- `CatalogService.cache` (HashMap<String, String>)
- ReadWriteLock (ReentrantReadWriteLock)

**Sequence Diagram:** `CatalogLookup_HappyPath`

---

## 3. Inventory Check Flow

**Flow Name:** Inventory Check  
**Endpoints:** `GET /api/inventory/{sku}`  
**Services:**
- ApiController
- InventoryService

**Data Structures:**
- `InventoryService.stock` (ConcurrentHashMap<String, Integer>)

**Sequence Diagram:** `InventoryCheck_HappyPath`

---

## 4. Admin Operations Flow

**Flow Name:** Admin Maintenance Mode Toggle  
**Endpoints:** 
- `POST /api/admin/maintenance`
- `GET /api/admin/maintenance`

**Services:**
- ApiController
- OrderService

**Data Structures:**
- `OrderService.maintenanceMode` (volatile boolean)

**Sequence Diagram:** `AdminMaintenance_HappyPath`

---

## 5. Load Testing Flow

**Flow Name:** Load Test Setup and Execution  
**Endpoints:**
- `POST /loadtest/setup`
- `POST /loadtest/run`

**Services:**
- LoadTestController
- FlashSaleGate
- OrderService
- InventoryService
- EventQueueWorker
- Metrics

**Data Structures:**
- `FlashSaleGate.barrier` (CyclicBarrier)
- `InventoryService.stock` (ConcurrentHashMap<String, Integer>)
- `InventoryService.locks` (ConcurrentHashMap<String, ReentrantLock>)
- `EventQueueWorker.queue` (BlockingQueue<Runnable>)
- `Metrics.*` (AtomicLong counters)

**Sequence Diagram:** `LoadTest_HappyPath`

---

## 6. Startup Flow

**Flow Name:** Application Startup Initialization  
**Endpoints:** N/A (triggered by @PostConstruct)  
**Services:**
- ApiController (@PostConstruct)
- StartupGate
- CatalogService
- InventoryService

**Data Structures:**
- `StartupGate.readyLatch` (CountDownLatch)
- `CatalogService.cache` (HashMap<String, String>)
- `InventoryService.stock` (ConcurrentHashMap<String, Integer>)

**Sequence Diagram:** `StartupFlow_HappyPath`

---

## 7. Email Queue Processing Flow

**Flow Name:** Email Queue Background Processing  
**Endpoints:** N/A (triggered by OrderService)  
**Services:**
- OrderService
- EventQueueWorker
- Metrics

**Data Structures:**
- `EventQueueWorker.queue` (BlockingQueue<Runnable>)
- `Metrics.emailsQueued` (AtomicLong)
- `Metrics.emailQueueDropped` (AtomicLong)

**Sequence Diagram:** `EmailQueue_HappyPath`

---

## 8. Debug Operations Flow

**Flow Name:** Deadlock Demonstration  
**Endpoints:** `POST /debug/deadlock`  
**Services:**
- DebugController

**Data Structures:**
- Local Object instances (A, B)
- Thread synchronization

**Sequence Diagram:** `DebugDeadlock_HappyPath`

---

**Flow Name:** Deadlock Fix Demonstration  
**Endpoints:** `POST /debug/deadlock/fix`  
**Services:**
- DebugController

**Data Structures:**
- Local Object instances (A, B)
- Thread synchronization (consistent ordering)

**Sequence Diagram:** `DebugDeadlockFix_HappyPath`

---

## 9. Metrics Collection Flow

**Flow Name:** Metrics Retrieval  
**Endpoints:** `GET /api/metrics`  
**Services:**
- ApiController
- Metrics

**Data Structures:**
- `Metrics.ordersPlaced` (AtomicLong)
- `Metrics.outOfStock` (AtomicLong)
- `Metrics.paymentsFailed` (AtomicLong)
- `Metrics.emailsQueued` (AtomicLong)
- `Metrics.emailQueueDropped` (AtomicLong)

**Sequence Diagram:** N/A (simple read operation)

---

## 10. Startup Status Check Flow

**Flow Name:** Startup Remaining Steps Check  
**Endpoints:** `GET /api/startup/remaining`  
**Services:**
- ApiController
- StartupGate

**Data Structures:**
- `StartupGate.readyLatch` (CountDownLatch)

**Sequence Diagram:** N/A (simple read operation)

---

## Summary Statistics

- **Total Endpoints:** 11
- **Total Business Flows:** 10
- **Total Sequence Diagrams:** 11
- **Services Involved:** 8
  - ApiController
  - DebugController
  - LoadTestController
  - OrderService
  - InventoryService
  - CatalogService
  - EventQueueWorker
  - StartupGate
  - FlashSaleGate
  - Metrics

- **Data Structures:** 6 in-memory structures
  - CatalogService.cache (HashMap)
  - InventoryService.stock (ConcurrentHashMap)
  - InventoryService.locks (ConcurrentHashMap)
  - EventQueueWorker.queue (BlockingQueue)
  - Metrics (AtomicLong counters)
  - OrderService.maintenanceMode (volatile boolean)
