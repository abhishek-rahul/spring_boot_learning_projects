# FlashKart E-Commerce Platform - Project Summary
## Phase 1-3 Implementation

**Project:** FlashKart  
**Version:** 0.0.1-SNAPSHOT  
**Spring Boot:** 4.0.1 | **Java:** 21

---

## Overview

Production-ready Spring Boot foundation with standardized API responses, global exception handling, request tracking, logging, API docs, and health monitoring.

### What's Built ✅

- Standardized API response structure
- Global exception handling
- Request ID tracking (observability)
- Structured logging with correlation
- Swagger/OpenAPI documentation
- Health monitoring (Actuator)
- Basic auth endpoint (stub)

---

## Architecture Flow

```
┌─────────────────────────────────────────────────────────────┐
│                    REQUEST FLOW                               │
└─────────────────────────────────────────────────────────────┘

Client Request
    │
    ▼
┌──────────────────────┐
│  RequestIdFilter     │  • Generate/extract Request ID
│  (Observability)     │  • Store in MDC for logging
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│  AuthController      │  • Validate request
│  (API Layer)          │  • Process login
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│  GlobalException     │  • Handle validation errors → 400
│  Handler             │  • Handle business errors → 404/409/etc
│                      │  • Handle unknown errors → 500
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│  Response Formatting │  • Success: ApiResponse<T>
│                      │  • Error: ApiErrorResponse
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│  Logging             │  • Log with Request ID
│  (Logback)           │  • Pattern: timestamp [thread] level [requestId] logger - msg
└──────────────────────┘
```

---

## Component Structure

### Shared Components (`com.flashkart.shared`)

**API Models:**
- `ApiResponse<T>` - Success response wrapper
- `ApiErrorResponse` - Error response wrapper
- `ApiError` - Field-level validation errors
- `PaginationMeta` - Pagination metadata

**Error Handling:**
- `ErrorCode` - Error code enum (VALIDATION_ERROR, NOT_FOUND, CONFLICT, etc.)
- `BusinessException` - Base business exception
- `NotFoundException`, `ConflictException` - Specific exceptions
- `GlobalExceptionHandler` - Centralized exception handling

**Observability:**
- `RequestIdFilter` - Generates/tracks Request IDs
- `CorrelationId` - Request ID constants

**Configuration:**
- `OpenApiConfig` - Swagger/OpenAPI setup

### Identity Module (`com.flashkart.identity`)

**API:**
- `AuthController` - `POST /api/v1/auth/login` (stub)

**DTOs:**
- `LoginRequest` - Login request with validation
- `TokenResponse` - Token response

---

## Request Flow Example

### Login Request Flow

```
1. POST /api/v1/auth/login
   {
     "email": "user@example.com",
     "password": "password123"
   }

2. RequestIdFilter
   • Generates UUID: "abc-123-xyz"
   • Stores in MDC
   • Adds to response header

3. AuthController
   • Validates request (@Valid)
   • Logs: "Login attempt for user: user@example.com"
   • Returns ApiResponse with tokens

4. If Error Occurs:
   • GlobalExceptionHandler catches it
   • Returns ApiErrorResponse with proper status code

5. Response:
   {
     "success": true,
     "apiVersion": "v1",
     "requestId": "abc-123-xyz",
     "timestamp": "2024-01-15T10:30:45.123Z",
     "data": {
       "accessToken": "access-token",
       "refreshToken": "refresh-token"
     }
   }

6. Log Output:
   2024-01-15 10:30:45.123 [http-nio-8080-exec-1] INFO [abc-123-xyz] 
   c.f.i.a.AuthController - Login attempt for user: user@example.com
```

---

## API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/v1/auth/login` | POST | User login (stub) |
| `/actuator/health` | GET | Health check |
| `/actuator/info` | GET | App information |
| `/swagger-ui.html` | GET | Swagger UI |
| `/v3/api-docs` | GET | OpenAPI spec |

---

## Error Handling

### Error Response Format

```json
{
  "success": false,
  "apiVersion": "v1",
  "requestId": "abc-123-xyz",
  "timestamp": "2024-01-15T10:30:45.123Z",
  "path": "/api/v1/auth/login",
  "httpStatus": 400,
  "errorCode": "VALIDATION_ERROR",
  "errorMessage": "Validation failed",
  "errorDetails": [
    {
      "field": "email",
      "message": "must be a well-formed email address"
    }
  ],
  "retryable": false
}
```

### Error Code → HTTP Status Mapping

| ErrorCode | HTTP Status |
|-----------|-------------|
| VALIDATION_ERROR | 400 |
| UNAUTHORIZED | 401 |
| FORBIDDEN | 403 |
| NOT_FOUND | 404 |
| CONFLICT | 409 |
| RATE_LIMITED | 429 |
| INTERNAL_ERROR | 500 |

---

## Logging

**Pattern:**
```
%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level [%X{requestId}] %logger{36} - %msg%n
```

**Example:**
```
2024-01-15 10:30:45.123 [http-nio-8080-exec-1] INFO [abc-123-xyz] 
c.f.i.a.AuthController - Login attempt for user: user@example.com
```

**Features:**
- Request ID automatically included from MDC
- Root level: INFO
- Console output

---

## Project Structure

```
flashkart/
├── src/main/java/com/flashkart/
│   ├── FlashkartApplication.java
│   ├── identity/
│   │   ├── api/
│   │   │   ├── AuthController.java
│   │   │   └── dto/
│   │   │       ├── LoginRequest.java
│   │   │       └── TokenResponse.java
│   │   ├── domain/ (empty)
│   │   ├── infra/ (empty)
│   │   └── service/ (empty)
│   └── shared/
│       ├── api/ (ApiResponse, ApiErrorResponse, etc.)
│       ├── error/ (Exception handling)
│       ├── observability/ (RequestIdFilter)
│       └── web/ (OpenApiConfig)
├── src/main/resources/
│   ├── application.yml
│   └── logback-spring.xml
└── pom.xml
```

---

## Key Features

✅ **Standardized Responses** - Consistent API response format  
✅ **Global Exception Handling** - Centralized error handling  
✅ **Request Correlation** - Request ID tracking for debugging  
✅ **Structured Logging** - Logs with request ID correlation  
✅ **API Documentation** - Swagger UI integration  
✅ **Health Monitoring** - Actuator endpoints  

---

## Dependencies

- Spring Boot Web
- Spring Boot Validation
- Spring Boot Actuator
- SpringDoc OpenAPI (Swagger)
- Spring Boot DevTools

---

## Summary

**Phase 1-3** establishes a production-ready foundation with:
- Consistent API structure
- Request tracking and observability
- Centralized error handling
- API documentation
- Health monitoring

**Ready for Phase 4:**
- Business logic implementation
- Service layer
- Repository/data access
- Domain models
- Database integration
- Complete authentication

---

**Document Version:** 1.0  
**Last Updated:** 2024

