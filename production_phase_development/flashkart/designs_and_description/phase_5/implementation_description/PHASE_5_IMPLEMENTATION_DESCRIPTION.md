# Phase 5 Implementation - Catalog Module Enhancement

## Overview

This document describes the implementation of Phase 5: **Catalog (Products) + Query Patterns** for the FlashKart e-commerce platform. The implementation focuses on production-grade catalog functionality with proper database discipline, query optimization, and caching strategies.

## Implementation Summary

### Goals Achieved ✅

1. **Product + SKU + Category + Price Versioning** - Complete domain model with relationships
2. **Pagination/Sorting/Filtering** - Full support for product search and listing
3. **Repository Patterns** - Demonstrated all patterns: derived queries, JPQL, native queries, and projections
4. **ORM Fundamentals** - Proper lazy/eager loading with N+1 query prevention
5. **Auditing Fields + Soft Delete** - Complete audit trail and soft delete support
6. **Caching (Basic)** - Cache-aside pattern for product details

## Architecture & Design Principles

### Clean Architecture
- **Domain Layer**: Pure business entities (Product, Sku, Category, Price)
- **Infrastructure Layer**: Repository interfaces and implementations
- **Service Layer**: Business logic and orchestration
- **API Layer**: Controllers and DTOs

### SOLID Principles Applied

1. **Single Responsibility**: Each class has one clear purpose
   - `ProductService`: Product business logic
   - `CacheService`: Caching operations
   - `ProductRepository`: Data access for products

2. **Open/Closed**: Extensible through interfaces and specifications
   - `ProductSpecification`: Dynamic query building
   - Repository interfaces allow extension without modification

3. **Liskov Substitution**: Repository interfaces can be substituted
   - All repositories extend `JpaRepository` and follow Spring Data contracts

4. **Interface Segregation**: Focused interfaces
   - `ProductProjection`: Only exposes needed fields
   - Separate DTOs for different use cases (ProductResponse, ProductSummaryResponse)

5. **Dependency Inversion**: Depend on abstractions
   - Services depend on repository interfaces, not implementations
   - Cache service is injected as an interface

## Key Components

### 1. Domain Entities

#### Product Entity
- **Purpose**: Represents a product in the catalog
- **Features**:
  - Soft delete support via `BaseAuditEntity`
  - Lazy loading for category and SKUs (prevents N+1)
  - Active/inactive flag for business logic

#### SKU Entity
- **Purpose**: Represents product variants (size, color, etc.)
- **Features**:
  - Stock quantity tracking
  - Reserved quantity for pending orders
  - Current price reference (lazy loaded)

#### Category Entity
- **Purpose**: Product categorization
- **Features**:
  - Hierarchical support ready
  - Active/inactive flag
  - Soft delete support

#### Price Entity
- **Purpose**: Price versioning for audit trail
- **Features**:
  - Version tracking
  - Current price flag
  - Historical price maintenance

### 2. Repository Patterns Demonstrated

#### Derived Query Methods
```java
// Example: Find active products by category
Page<Product> findByCategoryIdAndActiveTrueAndDeletedAtIsNull(UUID categoryId, Pageable pageable);
```
- **Use Case**: Simple queries with clear naming conventions
- **Benefits**: Type-safe, compile-time checked, easy to read

#### JPQL Queries
```java
// Example: Search with JOIN FETCH to avoid N+1
@Query("SELECT p FROM Product p " +
       "JOIN FETCH p.category " +
       "WHERE p.active = true AND p.deletedAt IS NULL")
List<Product> findActiveProductsWithCategory();
```
- **Use Case**: Complex queries with eager loading
- **Benefits**: Prevents N+1 queries, explicit control over loading

#### Native Queries
```java
// Example: Full-text search using PostgreSQL features
@Query(value = "SELECT p.* FROM products p " +
               "WHERE to_tsvector('english', p.name) @@ plainto_tsquery('english', :searchTerm)",
       nativeQuery = true)
List<Product> fullTextSearch(@Param("searchTerm") String searchTerm, Pageable pageable);
```
- **Use Case**: Database-specific features (PostgreSQL full-text search)
- **Benefits**: Leverage database capabilities, performance optimization

#### Projections
```java
// Example: Select only required fields
@Query("SELECT p.id as id, p.name as name, ... " +
       "MIN(pr.amount) as minPrice, MAX(pr.amount) as maxPrice " +
       "FROM Product p ...")
Page<ProductProjection> findProductSummaries(...);
```
- **Use Case**: Listing pages where full entity is not needed
- **Benefits**: Reduced memory footprint, faster queries, better performance

#### JPA Specifications
```java
// Example: Dynamic query building
public static Specification<Product> hasFilters(
        UUID categoryId, String brand, String searchTerm, ...) {
    return (root, query, cb) -> {
        // Build predicates dynamically
    };
}
```
- **Use Case**: Complex dynamic filtering
- **Benefits**: Reusable, composable, type-safe query building

### 3. N+1 Query Prevention

#### Problem
Without proper loading strategies, accessing related entities causes multiple queries:
```java
// BAD: Causes N+1 queries
List<Product> products = productRepository.findAll();
for (Product p : products) {
    p.getCategory().getName(); // N queries for categories
}
```

#### Solution
Using JOIN FETCH in JPQL:
```java
// GOOD: Single query with eager loading
@Query("SELECT p FROM Product p JOIN FETCH p.category WHERE ...")
List<Product> findActiveProductsWithCategory();
```

#### Implementation Examples
1. **Product with Category**: `findByIdWithCategory()` uses `LEFT JOIN FETCH`
2. **SKU with Price**: `findByProductIdWithPrice()` uses `LEFT JOIN FETCH`
3. **SKU with All Relations**: `findByIdWithRelations()` loads product, category, and price in one query

### 4. Caching Strategy (Cache-Aside Pattern)

#### Pattern Flow
```
1. Check cache for product
2. If found (cache hit), return cached data
3. If not found (cache miss), query database
4. Store result in cache for future requests
5. Return result
```

#### Implementation
- **Cache Key**: `catalog:product:{productId}`
- **TTL**: 1 hour (configurable)
- **Invalidation**: On product update/delete
- **Storage**: Redis

#### Benefits
- Reduces database load
- Improves response time for frequently accessed products
- Graceful degradation if cache fails

### 5. Soft Delete Implementation

#### BaseAuditEntity
All entities extend `BaseAuditEntity` which provides:
- `createdAt`: Timestamp when entity was created
- `updatedAt`: Timestamp when entity was last updated
- `deletedAt`: Timestamp when entity was soft deleted (null if active)

#### Soft Delete Methods
```java
product.softDelete(); // Sets deletedAt timestamp
product.restore();    // Clears deletedAt timestamp
```

#### Query Filtering
All repository queries include `deletedAt IS NULL` condition:
```java
Page<Product> findByActiveTrueAndDeletedAtIsNull(Pageable pageable);
```

### 6. Pagination, Sorting, and Filtering

#### Pagination
- Uses Spring Data `Pageable` interface
- Default: 20 items per page
- Configurable page size

#### Sorting
- Supports sorting by any product field
- Default: `createdAt DESC`
- Direction: ASC or DESC

#### Filtering
- **Category**: Filter by category ID
- **Brand**: Filter by brand name
- **Search Term**: Full-text search on product name
- **Active Status**: Filter active/inactive products

## API Endpoints

### Product Endpoints

#### GET `/api/v1/products/{id}`
- **Purpose**: Get detailed product information
- **Features**: 
  - Cache-aside pattern
  - Eager loading of category and SKUs
  - Includes price information

#### GET `/api/v1/products`
- **Purpose**: Search and list products
- **Query Parameters**:
  - `categoryId` (optional): Filter by category
  - `brand` (optional): Filter by brand
  - `searchTerm` (optional): Search in product name
  - `page` (default: 0): Page number
  - `size` (default: 20): Page size
  - `sortBy` (default: createdAt): Sort field
  - `sortDirection` (default: DESC): Sort direction

### Category Endpoints

#### GET `/api/v1/categories/{id}`
- **Purpose**: Get category details

#### GET `/api/v1/categories`
- **Purpose**: List all active categories

## Testing Strategy

### Unit Tests
- **ProductServiceTest**: Tests service layer logic, caching, error handling
- Uses Mockito for mocking dependencies

### Integration Tests
- **ProductRepositoryTest**: Tests repository queries (derived, JPQL, native)
- **SkuRepositoryTest**: Tests N+1 prevention with JOIN FETCH
- **PriceRepositoryTest**: Tests price versioning functionality
- Uses `@DataJpaTest` with H2 in-memory database

### Test Coverage
- Repository patterns (derived, JPQL, native, projections)
- N+1 query prevention
- Cache-aside pattern
- Soft delete functionality
- Pagination and sorting
- Error handling

## Performance Optimizations

1. **Projections**: Use DTOs/projections for listing pages
2. **JOIN FETCH**: Eager loading to prevent N+1 queries
3. **Caching**: Cache frequently accessed products
4. **Indexes**: Database indexes on frequently queried fields
5. **Pagination**: Limit result sets to manageable sizes

## Database Schema

### Key Tables
- `categories`: Product categories
- `products`: Product master data
- `skus`: Product variants
- `prices`: Price history with versioning

### Indexes
- `idx_products_category`: Category lookup
- `idx_products_active`: Active product filter
- `idx_products_name_search`: Full-text search (GIN index)
- `idx_prices_current`: Current price lookup
- `idx_skus_product`: Product-SKU relationship

## Code Quality

### Documentation
- Comprehensive JavaDoc for all public methods
- Inline comments explaining complex logic
- README-style documentation in code

### Error Handling
- Custom exceptions (`NotFoundException`)
- Proper HTTP status codes
- Meaningful error messages

### Code Organization
- Clear package structure
- Separation of concerns
- Consistent naming conventions

## Future Enhancements

1. **Elasticsearch Integration**: For advanced search capabilities
2. **CQRS Pattern**: Separate read/write models
3. **Event Sourcing**: Track all product changes
4. **Multi-tenancy**: Support for multiple tenants
5. **GraphQL API**: Flexible querying
6. **Rate Limiting**: Protect catalog endpoints
7. **Analytics**: Track product views, searches

## Migration Notes

### Database Migrations
- All schema changes are in Flyway migrations
- Migration: `V3__create_catalog_tables.sql`

### Configuration
- Redis configuration for caching
- JPA configuration for query optimization
- Test profile with H2 database

## Conclusion

Phase 5 implementation provides a production-ready catalog module with:
- ✅ Complete domain model
- ✅ Multiple repository patterns
- ✅ N+1 query prevention
- ✅ Caching strategy
- ✅ Comprehensive testing
- ✅ Clean architecture
- ✅ SOLID principles

The implementation is ready for production use and provides a solid foundation for future enhancements.
