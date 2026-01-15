# FlashKart Design Documentation

## Step 1: Discovery

### REST Endpoints

#### Identity Module (`/api/v1/auth`)
- `POST /api/v1/auth/signup` - User registration
- `POST /api/v1/auth/login` - User login (JWT)
- `POST /api/v1/auth/refresh` - Refresh access token
- `POST /api/v1/auth/logout` - Logout (single device)
- `POST /api/v1/auth/logout-all` - Logout all devices

#### Identity Module (`/api/v1/users`)
- `GET /api/v1/users/{userId}` - Get user by ID (RBAC/ABAC)
- `GET /api/v1/users` - List all users (Admin only)

#### Identity Module (`/api/v1/me`)
- `GET /api/v1/me` - Get current user info (JWT)

#### Admin Module (`/api/v1/admin/auth`)
- `POST /api/v1/admin/auth/login` - Admin login (Session-based)

#### Admin Module (`/api/v1/admin`)
- `GET /api/v1/admin/me` - Get current admin info
- `GET /api/v1/admin/csrf` - Get CSRF token

#### Catalog Module (`/api/v1/categories`)
- `GET /api/v1/categories/{id}` - Get category by ID
- `GET /api/v1/categories` - Get all active categories

#### Catalog Module (`/api/v1/products`)
- `GET /api/v1/products/{id}` - Get product by ID
- `GET /api/v1/products` - Search products (pagination, sorting, filtering)

#### Cart Module (`/api/v1/carts`)
- `GET /api/v1/carts` - Get current user's cart
- `POST /api/v1/carts/items` - Add item to cart
- `PUT /api/v1/carts/items/{skuId}` - Update item quantity
- `DELETE /api/v1/carts/items/{skuId}` - Remove item from cart
- `DELETE /api/v1/carts/items` - Clear all items
- `DELETE /api/v1/carts` - Delete cart

#### Order Module (`/api/v1/orders`)
- `GET /api/v1/orders/{orderId}` - Get order by ID (RBAC/ABAC)

#### System Endpoints
- `GET /actuator/health` - Health check
- `GET /actuator/info` - Application info
- `GET /swagger-ui.html` - Swagger UI
- `GET /v3/api-docs` - OpenAPI spec

### Async Entrypoints

#### Scheduled Jobs
- `CartCleanupJob.cleanupExpiredCarts()` - Runs hourly (cron: `0 0 * * * ?`)
  - Uses distributed lock via Redis
  - Soft deletes expired carts

#### Kafka Listeners
- **None found** (not implemented yet)

#### @Async Methods
- **None found** (not implemented yet)

### External Integrations

#### Redis
- **Purpose**: Caching, distributed locking, session storage
- **Usage**:
  - `CartCacheService` - Cart caching (cache-aside pattern)
  - `CacheService` (catalog) - Product caching
  - `DistributedLockService` - Distributed job locking
  - `RedisAtomicCounter` - Rate limiting counters
  - `LoginThrottleService` - Login attempt throttling
  - Spring Session - Admin session storage

#### Payment Gateway
- **Not implemented yet**

#### Email Service
- **Not implemented yet**

#### SMS Service
- **Not implemented yet**

### Persistence Layer

#### JPA Entities
- `User` → `users` table
- `RefreshToken` → `refresh_tokens` table
- `Category` → `categories` table
- `Product` → `products` table
- `Sku` → `skus` table
- `Price` → `prices` table
- `Cart` → `carts` table
- `CartItem` → `cart_items` table

#### Repositories
- `UserRepository`
- `RefreshTokenRepository`
- `CategoryRepository`
- `ProductRepository`
- `ProductSummaryRepository`
- `SkuRepository`
- `PriceRepository`
- `CartRepository`
- `CartItemRepository`
- `OrderRepository` (stub)

#### Database Migrations (Flyway)
- `V1__create_users.sql` - Users and user_roles tables
- `V2__create_refresh_tokens.sql` - Refresh tokens table
- `V3__create_catalog_tables.sql` - Categories, products, skus, prices tables
- `V4__create_cart_tables.sql` - Carts and cart_items tables

---

## Step 2: Business Flows

### Flow Groups

1. **Authentication & Authorization**
   - User Signup
   - User Login
   - Token Refresh
   - Admin Login
   - Logout

2. **Catalog**
   - Get Category
   - Search Products
   - Get Product Details

3. **Cart**
   - Get Cart
   - Add Item to Cart
   - Update Cart Item
   - Remove Item from Cart
   - Cart Cleanup (Scheduled Job)

4. **Order** (Stub - minimal implementation)

---

## Step 3: Sequence Diagrams

See `SEQUENCE_DIAGRAMS.puml` for all sequence diagrams.

---

## Step 4: Database ERD

See `DATABASE_ERD.puml` for the complete database model.

---

## Step 5: Traceability

See `TRACEABILITY_TABLE.md` for the complete mapping.

