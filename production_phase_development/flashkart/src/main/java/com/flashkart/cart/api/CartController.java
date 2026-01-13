package com.flashkart.cart.api;

import com.flashkart.cart.api.dto.AddItemRequest;
import com.flashkart.cart.api.dto.CartItemResponse;
import com.flashkart.cart.api.dto.CartResponse;
import com.flashkart.cart.api.dto.UpdateItemQuantityRequest;
import com.flashkart.cart.domain.Cart;
import com.flashkart.cart.domain.CartItem;
import com.flashkart.cart.service.CartService;
import com.flashkart.shared.api.ApiResponse;
import com.flashkart.shared.observability.CorrelationId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Cart API controller.
 * Provides endpoints for cart CRUD operations.
 */
@RestController
@RequestMapping("/api/v1/carts")
@Tag(name = "Carts", description = "Shopping cart APIs")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    /**
     * Get current user's cart.
     */
    @GetMapping
    @Operation(summary = "Get cart", description = "Returns the current user's active cart")
    public ApiResponse<CartResponse> getCart(Authentication authentication) {
        UUID userId = getUserId(authentication);
        Cart cart = cartService.getCart(userId);
        String requestId = MDC.get(CorrelationId.MDC_KEY);
        return ApiResponse.ok("v1", requestId, toCartResponse(cart));
    }

    /**
     * Add item to cart.
     */
    @PostMapping("/items")
    @Operation(summary = "Add item to cart", description = "Adds a product SKU to the cart or updates quantity if already exists")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CartResponse> addItem(
            @Valid @RequestBody AddItemRequest request,
            Authentication authentication) {
        UUID userId = getUserId(authentication);
        Cart cart = cartService.addItemToCart(userId, request.skuId(), request.quantity());
        String requestId = MDC.get(CorrelationId.MDC_KEY);
        return ApiResponse.ok("v1", requestId, toCartResponse(cart));
    }

    /**
     * Update item quantity in cart.
     */
    @PutMapping("/items/{skuId}")
    @Operation(summary = "Update item quantity", description = "Updates the quantity of a specific item in the cart")
    public ApiResponse<CartResponse> updateItemQuantity(
            @Parameter(description = "SKU ID", required = true)
            @PathVariable UUID skuId,
            @Valid @RequestBody UpdateItemQuantityRequest request,
            Authentication authentication) {
        UUID userId = getUserId(authentication);
        Cart cart = cartService.updateItemQuantity(userId, skuId, request.quantity());
        String requestId = MDC.get(CorrelationId.MDC_KEY);
        return ApiResponse.ok("v1", requestId, toCartResponse(cart));
    }

    /**
     * Remove item from cart.
     */
    @DeleteMapping("/items/{skuId}")
    @Operation(summary = "Remove item from cart", description = "Removes a specific item from the cart")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeItem(
            @Parameter(description = "SKU ID", required = true)
            @PathVariable UUID skuId,
            Authentication authentication) {
        UUID userId = getUserId(authentication);
        cartService.removeItemFromCart(userId, skuId);
    }

    /**
     * Clear all items from cart.
     */
    @DeleteMapping("/items")
    @Operation(summary = "Clear cart", description = "Removes all items from the cart")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearCart(Authentication authentication) {
        UUID userId = getUserId(authentication);
        cartService.clearCart(userId);
    }

    /**
     * Delete cart.
     */
    @DeleteMapping
    @Operation(summary = "Delete cart", description = "Soft deletes the cart")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCart(Authentication authentication) {
        UUID userId = getUserId(authentication);
        cartService.deleteCart(userId);
    }

    /**
     * Extract user ID from authentication context.
     */
    private UUID getUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt)) {
            throw new IllegalStateException("User not authenticated");
        }
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String subject = jwt.getSubject();
        return UUID.fromString(subject);
    }

    /**
     * Convert Cart entity to CartResponse DTO.
     */
    private CartResponse toCartResponse(Cart cart) {
        // Load items with SKU details for response
        List<CartItemResponse> itemResponses = cart.getItems().stream()
                .map(this::toCartItemResponse)
                .collect(Collectors.toList());

        return new CartResponse(
                cart.getId(),
                cart.getUserId(),
                itemResponses,
                cart.getTotalItemCount(),
                cart.getExpiresAt(),
                cart.getCreatedAt(),
                cart.getUpdatedAt()
        );
    }

    /**
     * Convert CartItem entity to CartItemResponse DTO.
     */
    private CartItemResponse toCartItemResponse(CartItem item) {
        return new CartItemResponse(
                item.getId(),
                item.getSkuId(),
                item.getQuantity(),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }
}

