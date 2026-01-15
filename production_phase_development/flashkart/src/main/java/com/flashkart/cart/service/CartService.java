package com.flashkart.cart.service;

import com.flashkart.cart.domain.Cart;
import com.flashkart.cart.domain.CartItem;
import com.flashkart.cart.infra.CartItemRepository;
import com.flashkart.cart.infra.CartRepository;
import com.flashkart.catalog.domain.Sku;
import com.flashkart.catalog.infra.SkuRepository;
import com.flashkart.shared.error.ErrorCode;
import com.flashkart.shared.error.NotFoundException;
import com.flashkart.shared.error.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Cart service implementing cart CRUD operations, quantity validation, and expiry logic.
 * Follows Clean Architecture principles with clear separation of concerns.
 */
@Service
@Transactional
public class CartService {

    private static final Logger logger = LoggerFactory.getLogger(CartService.class);
    
    // Cart expiry configuration: 30 days of inactivity
    private static final int CART_EXPIRY_DAYS = 30;

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final SkuRepository skuRepository;
    private final CartCacheService cartCacheService;

    public CartService(
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            SkuRepository skuRepository,
            CartCacheService cartCacheService) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.skuRepository = skuRepository;
        this.cartCacheService = cartCacheService;
    }

    /**
     * Get or create an active cart for a user.
     * Uses cache-aside pattern for reads.
     */
    public Cart getOrCreateCart(UUID userId) {
        Instant now = Instant.now();
        
        // Try cache first
        Optional<Cart> cachedCart = cartCacheService.getCart(userId, Cart.class);
        if (cachedCart.isPresent()) {
            Cart cart = cachedCart.get();
            if (!cart.isExpired()) {
                // Load items for cached cart
                List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
                cart.getItems().clear();
                cart.getItems().addAll(items);
                return cart;
            }
        }

        // Cache miss or expired - check database
        Optional<Cart> existingCart = cartRepository.findActiveCartByUserId(userId, now);
        
        if (existingCart.isPresent()) {
            Cart cart = existingCart.get();
            // Load items
            List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
            cart.getItems().clear();
            cart.getItems().addAll(items);
            // Refresh cache
            cartCacheService.putCart(userId, cart);
            return cart;
        }

        // Create new cart
        Instant expiresAt = now.plus(CART_EXPIRY_DAYS, ChronoUnit.DAYS);
        Cart newCart = new Cart(userId, expiresAt);
        Cart savedCart = cartRepository.save(newCart);
        
        // Cache the new cart
        cartCacheService.putCart(userId, savedCart);
        
        logger.info("Created new cart for user: {}", userId);
        return savedCart;
    }

    /**
     * Get active cart for a user (read-only).
     */
    @Transactional(readOnly = true)
    public Cart getCart(UUID userId) {
        Instant now = Instant.now();
        
        // Try cache first
        Optional<Cart> cachedCart = cartCacheService.getCart(userId, Cart.class);
        if (cachedCart.isPresent()) {
            Cart cart = cachedCart.get();
            if (!cart.isExpired()) {
                // Load items for cached cart
                List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
                cart.getItems().clear();
                cart.getItems().addAll(items);
                return cart;
            }
        }

        // Cache miss - check database
        Cart cart = cartRepository.findActiveCartByUserId(userId, now)
                .orElseThrow(() -> new NotFoundException("Cart not found for user: " + userId));
        
        // Load items
        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
        cart.getItems().clear();
        cart.getItems().addAll(items);
        
        return cart;
    }

    /**
     * Add item to cart or update quantity if item already exists.
     */
    public Cart addItemToCart(UUID userId, UUID skuId, Integer quantity) {
        validateQuantity(quantity);
        
        Cart cart = getOrCreateCart(userId);
        
        // Load cart items to check for existing item
        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
        cart.getItems().clear();
        cart.getItems().addAll(items);
        
        // Check if item already exists in cart
        Optional<CartItem> existingItem = items.stream()
                .filter(item -> item.getSkuId().equals(skuId))
                .findFirst();

        if (existingItem.isPresent()) {
            // Update quantity
            CartItem item = existingItem.get();
            int newQuantity = item.getQuantity() + quantity;
            validateSkuAvailability(skuId, newQuantity);
            item.setQuantity(newQuantity);
            cartItemRepository.save(item);
        } else {
            // Add new item
            validateSkuAvailability(skuId, quantity);
            CartItem newItem = new CartItem(cart, skuId, quantity);
            cartItemRepository.save(newItem);
            cart.addItem(newItem);
        }

        // Extend cart expiry on activity
        extendCartExpiry(cart);
        
        // Invalidate cache
        cartCacheService.invalidateCart(userId);
        
        logger.info("Added item to cart: userId={}, skuId={}, quantity={}", userId, skuId, quantity);
        return cartRepository.findById(cart.getId()).orElse(cart);
    }

    /**
     * Update item quantity in cart.
     */
    public Cart updateItemQuantity(UUID userId, UUID skuId, Integer quantity) {
        validateQuantity(quantity);
        validateSkuAvailability(skuId, quantity);
        
        Cart cart = getCart(userId);
        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
        
        CartItem item = items.stream()
                .filter(i -> i.getSkuId().equals(skuId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Cart item not found for SKU: " + skuId));

        item.setQuantity(quantity);
        cartItemRepository.save(item);
        
        // Extend cart expiry on activity
        extendCartExpiry(cart);
        
        // Invalidate cache
        cartCacheService.invalidateCart(userId);
        
        logger.info("Updated cart item quantity: userId={}, skuId={}, quantity={}", userId, skuId, quantity);
        return cartRepository.findById(cart.getId()).orElse(cart);
    }

    /**
     * Remove item from cart.
     */
    public void removeItemFromCart(UUID userId, UUID skuId) {
        Cart cart = getCart(userId);
        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
        
        CartItem item = items.stream()
                .filter(i -> i.getSkuId().equals(skuId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Cart item not found for SKU: " + skuId));

        item.softDelete();
        cartItemRepository.save(item);
        
        // Extend cart expiry on activity
        extendCartExpiry(cart);
        
        // Invalidate cache
        cartCacheService.invalidateCart(userId);
        
        logger.info("Removed item from cart: userId={}, skuId={}", userId, skuId);
    }

    /**
     * Clear all items from cart.
     */
    public void clearCart(UUID userId) {
        Cart cart = getCart(userId);
        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
        
        items.forEach(CartItem::softDelete);
        cartItemRepository.saveAll(items);
        
        // Invalidate cache
        cartCacheService.invalidateCart(userId);
        
        logger.info("Cleared cart for user: {}", userId);
    }

    /**
     * Delete cart (soft delete).
     */
    public void deleteCart(UUID userId) {
        Cart cart = getCart(userId);
        cart.softDelete();
        cartRepository.save(cart);
        
        // Invalidate cache
        cartCacheService.invalidateCart(userId);
        
        logger.info("Deleted cart for user: {}", userId);
    }

    /**
     * Validate quantity is positive.
     */
    private void validateQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "Quantity must be greater than 0",
                    false
            );
        }
    }

    /**
     * Validate SKU exists and has sufficient available stock.
     */
    private void validateSkuAvailability(UUID skuId, Integer requestedQuantity) {
        Sku sku = skuRepository.findById(skuId)
                .orElseThrow(() -> new NotFoundException("SKU not found: " + skuId));

        if (!sku.isActive() || sku.isDeleted()) {
            throw new BusinessException(
                    ErrorCode.SKU_NOT_FOUND,
                    "SKU is not available",
                    false
            );
        }

        int availableQuantity = sku.getAvailableQuantity();
        if (requestedQuantity > availableQuantity) {
            throw new BusinessException(
                    ErrorCode.INSUFFICIENT_STOCK,
                    String.format("Insufficient stock. Available: %d, Requested: %d", availableQuantity, requestedQuantity),
                    false
            );
        }
    }

    /**
     * Extend cart expiry time on activity.
     */
    private void extendCartExpiry(Cart cart) {
        Instant newExpiresAt = Instant.now().plus(CART_EXPIRY_DAYS, ChronoUnit.DAYS);
        cart.setExpiresAt(newExpiresAt);
        cartRepository.save(cart);
    }
}

