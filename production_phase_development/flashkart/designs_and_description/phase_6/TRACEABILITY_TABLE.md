# FlashKart Traceability Table

## Mapping: Sequence Diagrams → Endpoints → Services → Tables

| Sequence Diagram | Endpoints | Services | Repositories | Database Tables | External Systems |
|-----------------|------------|----------|--------------|-----------------|------------------|
| **Auth_Signup_Happy** | `POST /api/v1/auth/signup` | `AuthService`, `UserService`, `JwtService`, `RefreshTokenService` | `UserRepository`, `RefreshTokenRepository` | `users`, `user_roles`, `refresh_tokens` | - |
| **Auth_Login_Happy** | `POST /api/v1/auth/login` | `AuthService`, `JwtService`, `RefreshTokenService`, `LoginThrottleService` | `UserRepository`, `RefreshTokenRepository` | `users`, `user_roles`, `refresh_tokens` | Redis (rate limiting, throttling) |
| **Auth_Login_Failure** | `POST /api/v1/auth/login` | `AuthService`, `LoginThrottleService` | `UserRepository` | `users` | Redis (throttling) |
| **Auth_Refresh_Happy** | `POST /api/v1/auth/refresh` | `RefreshTokenService`, `JwtService` | `RefreshTokenRepository`, `UserRepository` | `refresh_tokens`, `users` | - |
| **Admin_Login_Happy** | `POST /api/v1/admin/auth/login` | `AuthenticationManager`, `AdminUserDetailsService` | `UserRepository` | `users`, `user_roles` | Redis (rate limiting, session storage) |
| **Catalog_GetProduct_Happy** | `GET /api/v1/products/{id}` | `ProductService`, `CacheService` | `ProductRepository` | `products`, `categories`, `skus`, `prices` | Redis (caching) |
| **Catalog_SearchProducts_Happy** | `GET /api/v1/products` | `ProductService` | `ProductRepository`, `ProductSpecification` | `products`, `categories` | - |
| **Cart_GetCart_Happy** | `GET /api/v1/carts` | `CartService`, `CartCacheService` | `CartRepository`, `CartItemRepository` | `carts`, `cart_items` | Redis (caching) |
| **Cart_AddItem_Happy** | `POST /api/v1/carts/items` | `CartService`, `CartCacheService` | `CartRepository`, `CartItemRepository`, `SkuRepository` | `carts`, `cart_items`, `skus` | Redis (cache invalidation) |
| **Cart_AddItem_Failure** | `POST /api/v1/carts/items` | `CartService` | `SkuRepository` | `skus` | - |
| **Cart_CleanupJob_Happy** | Scheduled (hourly) | `CartCleanupJob`, `DistributedLockService` | `CartRepository` | `carts` | Redis (distributed lock) |

---

## Detailed Class-to-Table Mapping

### Identity Module

| Class | Package | Table(s) | Operations |
|-------|---------|----------|------------|
| `User` | `com.flashkart.identity.domain` | `users`, `user_roles` | CREATE, READ, UPDATE |
| `UserRepository` | `com.flashkart.identity.infra` | `users`, `user_roles` | `findByEmail()`, `existsByEmail()`, `save()`, `findById()` |
| `RefreshToken` | `com.flashkart.identity.domain` | `refresh_tokens` | CREATE, READ, UPDATE (revoke) |
| `RefreshTokenRepository` | `com.flashkart.identity.infra` | `refresh_tokens` | `findByTokenHash()`, `save()`, `findByUserId()` |
| `AuthService` | `com.flashkart.identity.service` | `users` (via UserRepository) | `signup()`, `login()` |
| `RefreshTokenService` | `com.flashkart.identity.service` | `refresh_tokens` (via RefreshTokenRepository) | `issueToken()`, `rotateWithUser()`, `revoke()`, `revokeAllForUser()` |
| `JwtService` | `com.flashkart.identity.service` | - | Token generation (no DB) |

### Catalog Module

| Class | Package | Table(s) | Operations |
|-------|---------|----------|------------|
| `Category` | `com.flashkart.catalog.domain` | `categories` | CREATE, READ, UPDATE, SOFT DELETE |
| `CategoryRepository` | `com.flashkart.catalog.infra` | `categories` | `findById()`, `findByActiveTrueAndDeletedAtIsNull()`, `findByNameIgnoreCase()` |
| `Product` | `com.flashkart.catalog.domain` | `products` | CREATE, READ, UPDATE, SOFT DELETE |
| `ProductRepository` | `com.flashkart.catalog.infra` | `products`, `categories` | `findByIdWithRelations()`, `findAll(Specification, Pageable)` |
| `Sku` | `com.flashkart.catalog.domain` | `skus` | CREATE, READ, UPDATE, SOFT DELETE |
| `SkuRepository` | `com.flashkart.catalog.infra` | `skus`, `products`, `prices` | `findById()`, `findByIdWithRelations()`, `findBySkuCode()`, `findByProductIdWithPrice()` |
| `Price` | `com.flashkart.catalog.domain` | `prices` | CREATE, READ, UPDATE, SOFT DELETE |
| `PriceRepository` | `com.flashkart.catalog.infra` | `prices` | `findBySkuId()`, `findCurrentBySkuId()`, `save()` |
| `ProductService` | `com.flashkart.catalog.service` | `products`, `categories`, `skus`, `prices` (via repositories) | `getById()`, `searchProducts()` |
| `CategoryService` | `com.flashkart.catalog.service` | `categories` (via CategoryRepository) | `getById()`, `getAllActiveCategories()` |
| `CacheService` | `com.flashkart.catalog.service` | - | Redis operations (caching) |

### Cart Module

| Class | Package | Table(s) | Operations |
|-------|---------|----------|------------|
| `Cart` | `com.flashkart.cart.domain` | `carts` | CREATE, READ, UPDATE, SOFT DELETE |
| `CartRepository` | `com.flashkart.cart.infra` | `carts` | `findActiveCartByUserId()`, `findExpiredCarts()`, `softDeleteExpiredCarts()`, `save()` |
| `CartItem` | `com.flashkart.cart.domain` | `cart_items` | CREATE, READ, UPDATE, SOFT DELETE |
| `CartItemRepository` | `com.flashkart.cart.infra` | `cart_items` | `findByCartId()`, `findByCartIdAndSkuId()`, `save()`, `saveAll()` |
| `CartService` | `com.flashkart.cart.service` | `carts`, `cart_items`, `skus` (via repositories) | `getCart()`, `getOrCreateCart()`, `addItemToCart()`, `updateItemQuantity()`, `removeItemFromCart()`, `clearCart()`, `deleteCart()` |
| `CartCacheService` | `com.flashkart.cart.service` | - | Redis operations (caching) |
| `CartCleanupJob` | `com.flashkart.cart.job` | `carts` (via CartRepository) | `cleanupExpiredCarts()` (scheduled) |
| `DistributedLockService` | `com.flashkart.shared.infra` | - | Redis operations (distributed locking) |

### Admin Module

| Class | Package | Table(s) | Operations |
|-------|---------|----------|------------|
| `AdminUserDetailsService` | `com.flashkart.admin.security` | `users`, `user_roles` (via UserRepository) | `loadUserByUsername()` |
| `AdminAuthController` | `com.flashkart.admin.api` | - | Session management (Redis-backed) |

### Shared/Infrastructure

| Class | Package | Table(s) | Operations |
|-------|---------|----------|------------|
| `BaseAuditEntity` | `com.flashkart.shared.domain` | All tables (via inheritance) | Provides `created_at`, `updated_at`, `deleted_at` |
| `RequestIdFilter` | `com.flashkart.shared.observability` | - | Request tracking (no DB) |
| `GlobalExceptionHandler` | `com.flashkart.shared.error` | - | Error handling (no DB) |
| `RateLimitFilter` | `com.flashkart.shared.security.ratelimit` | - | Redis operations (rate limiting) |
| `LoginThrottleService` | `com.flashkart.shared.security.ratelimit` | - | Redis operations (throttling) |

---

## External System Interactions

| External System | Purpose | Classes Using It | Operations |
|----------------|---------|-----------------|------------|
| **Redis** | Caching | `CacheService`, `CartCacheService` | `GET`, `SET`, `DELETE` (with TTL) |
| **Redis** | Distributed Locking | `DistributedLockService`, `CartCleanupJob` | `SETNX`, `EVAL` (Lua script) |
| **Redis** | Rate Limiting | `RateLimitFilter`, `RedisAtomicCounter` | `INCR`, `EXPIRE` |
| **Redis** | Session Storage | Spring Session (admin) | Session CRUD |
| **Redis** | Login Throttling | `LoginThrottleService` | `GET`, `SET`, `INCR` |
| **PostgreSQL** | Primary Database | All repositories | SQL operations (via JPA/Hibernate) |

---

## Transaction Boundaries

| Service Method | @Transactional | Tables Involved | Isolation Level (Assumed) |
|---------------|----------------|-----------------|---------------------------|
| `AuthService.signup()` | Yes (inherited) | `users`, `user_roles` | READ_COMMITTED (default) |
| `RefreshTokenService.rotateWithUser()` | Yes | `refresh_tokens` | READ_COMMITTED (default) |
| `CartService.addItemToCart()` | Yes | `carts`, `cart_items`, `skus` | READ_COMMITTED (default) |
| `CartService.updateItemQuantity()` | Yes | `cart_items`, `carts` | READ_COMMITTED (default) |
| `CartService.removeItemFromCart()` | Yes | `cart_items`, `carts` | READ_COMMITTED (default) |
| `CartService.getCart()` | `@Transactional(readOnly = true)` | `carts`, `cart_items` | READ_COMMITTED (default) |
| `CartCleanupJob.cleanupExpiredCarts()` | Yes (implicit) | `carts` | READ_COMMITTED (default) |

**Note**: Explicit isolation levels are not defined in the codebase. Assumed to use Spring's default (READ_COMMITTED for PostgreSQL).

---

## Cache Keys and Patterns

| Cache Service | Key Pattern | TTL | Invalidation Trigger |
|---------------|-------------|-----|----------------------|
| `CacheService` (Catalog) | `catalog:product:{id}` | 1 hour | Manual (on product update) |
| `CartCacheService` | `cart:{userId}` | 1 hour | On all cart mutations |
| `DistributedLockService` | `lock:{lockKey}` | Configurable (default: 5 min) | On lock release |
| `RedisAtomicCounter` | `rl:{scope}:{key}` | Window-based | Auto-expire |
| `LoginThrottleService` | `throttle_block:{email}:{ip}` | Configurable | On success or timeout |

---

## Assumptions and Notes

1. **Kafka Integration**: Not implemented yet (mentioned in phase plan but not in codebase)
2. **Payment Gateway**: Not implemented yet
3. **Email/SMS Services**: Not implemented yet
4. **Order Module**: Minimal implementation (stub controller only)
5. **Isolation Levels**: Not explicitly defined; using Spring defaults
6. **Database**: PostgreSQL (confirmed from migrations and application.yml)
7. **Redis**: Used for caching, locking, rate limiting, sessions, throttling
8. **Soft Delete**: Implemented via `deleted_at` timestamp in most entities
9. **Audit Fields**: All entities extend `BaseAuditEntity` (except `User` and `RefreshToken` which have custom audit fields)

