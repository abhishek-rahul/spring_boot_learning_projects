package com.flashkart.order.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashkart.order.api.dto.CheckoutRequest;
import com.flashkart.order.api.dto.OrderItemResponse;
import com.flashkart.order.api.dto.OrderResponse;
import com.flashkart.order.api.dto.PaymentIntentResponse;
import com.flashkart.order.domain.Order;
import com.flashkart.order.domain.OrderItem;
import com.flashkart.order.domain.PaymentIntent;
import com.flashkart.order.service.CheckoutService;
import com.flashkart.order.service.IdempotencyService;
import com.flashkart.shared.api.ApiResponse;
import com.flashkart.shared.error.BusinessException;
import com.flashkart.shared.error.ErrorCode;
import com.flashkart.shared.observability.CorrelationId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Orders", description = "Order management APIs")
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    private final CheckoutService checkoutService;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    public OrderController(
            CheckoutService checkoutService,
            IdempotencyService idempotencyService,
            ObjectMapper objectMapper) {
        this.checkoutService = checkoutService;
        this.idempotencyService = idempotencyService;
        this.objectMapper = objectMapper;
    }

    /**
     * Checkout a cart and create an order.
     * Supports idempotency keys for safe retry.
     */
    @PostMapping("/checkout")
    @Operation(
            summary = "Checkout cart",
            description = "Creates an order from a cart. Supports idempotency keys for safe retry."
    )
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ApiResponse<OrderResponse>> checkout(
            @Valid @RequestBody CheckoutRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        
        UUID userId = getUserId(authentication);
        String requestId = MDC.get(CorrelationId.MDC_KEY);
        String requestPath = httpRequest.getRequestURI();

        // Handle idempotency if key is provided
        if (request.idempotencyKey() != null && !request.idempotencyKey().isBlank()) {
            IdempotencyService.IdempotencyResponse existingResponse = 
                    idempotencyService.getExistingResponse(
                            request.idempotencyKey(), 
                            userId, 
                            requestPath
                    ).orElse(null);

            if (existingResponse != null) {
                logger.info("Returning cached response for idempotency key: userId={}, key={}", 
                        userId, request.idempotencyKey());
                
                try {
                    OrderResponse cachedResponse = objectMapper.readValue(
                            existingResponse.getResponseBody(), 
                            OrderResponse.class
                    );
                    return ResponseEntity
                            .status(existingResponse.getStatusCode())
                            .body(ApiResponse.ok("v1", requestId, cachedResponse));
                } catch (Exception e) {
                    logger.error("Failed to deserialize cached response", e);
                    // Fall through to process normally
                }
            }
        }

        // Process checkout
        Order order;
        try {
            order = checkoutService.checkout(userId, request.cartId());
        } catch (BusinessException e) {
            // Store error response for idempotency if key provided
            if (request.idempotencyKey() != null && !request.idempotencyKey().isBlank()) {
                idempotencyService.storeResponse(
                        request.idempotencyKey(),
                        userId,
                        requestPath,
                        HttpStatus.BAD_REQUEST.value(),
                        Map.of("error", e.getMessage(), "errorCode", e.getErrorCode().name())
                );
            }
            throw e;
        }

        OrderResponse response = toOrderResponse(order);

        // Store successful response for idempotency if key provided
        if (request.idempotencyKey() != null && !request.idempotencyKey().isBlank()) {
            idempotencyService.storeResponse(
                    request.idempotencyKey(),
                    userId,
                    requestPath,
                    HttpStatus.CREATED.value(),
                    response
            );
        }

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok("v1", requestId, response));
    }

    /**
     * Get order by ID.
     */
    @GetMapping("/{orderId}")
    @Operation(summary = "Get order", description = "Returns order details by ID")
    public ApiResponse<OrderResponse> getOrder(
            @PathVariable UUID orderId,
            Authentication authentication) {
        UUID userId = getUserId(authentication);
        String requestId = MDC.get(CorrelationId.MDC_KEY);
        
        // TODO: Implement order retrieval in Phase 8 or later
        throw new BusinessException(
                ErrorCode.NOT_FOUND,
                "Order retrieval not yet implemented",
                false
        );
    }

    private UUID getUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new BusinessException(
                    ErrorCode.UNAUTHORIZED,
                    "User not authenticated",
                    false
            );
        }
        // Try userId claim first, fallback to subject
        String userIdStr = jwt.getClaim("userId");
        if (userIdStr == null) {
            userIdStr = jwt.getSubject();
        }
        if (userIdStr == null) {
            throw new BusinessException(
                    ErrorCode.UNAUTHORIZED,
                    "User ID not found in token",
                    false
            );
        }
        return UUID.fromString(userIdStr);
    }

    private OrderResponse toOrderResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(this::toOrderItemResponse)
                .collect(Collectors.toList());

        PaymentIntentResponse paymentIntentResponse = null;
        if (order.getPaymentIntent() != null) {
            paymentIntentResponse = toPaymentIntentResponse(order.getPaymentIntent());
        }

        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus().name(),
                order.getTotalAmount(),
                order.getCurrency(),
                order.getCartId(),
                items,
                paymentIntentResponse,
                order.getCreatedAt()
        );
    }

    private OrderItemResponse toOrderItemResponse(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getSkuId(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getTotalPrice(),
                item.getCurrency()
        );
    }

    private PaymentIntentResponse toPaymentIntentResponse(PaymentIntent paymentIntent) {
        return new PaymentIntentResponse(
                paymentIntent.getId(),
                paymentIntent.getAmount(),
                paymentIntent.getCurrency(),
                paymentIntent.getStatus().name(),
                paymentIntent.getPaymentProvider()
        );
    }
}
