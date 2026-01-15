package com.flashkart.order.service;

import com.flashkart.cart.domain.Cart;
import com.flashkart.cart.domain.CartItem;
import com.flashkart.cart.infra.CartItemRepository;
import com.flashkart.cart.infra.CartRepository;
import com.flashkart.catalog.domain.Price;
import com.flashkart.catalog.domain.Sku;
import com.flashkart.catalog.infra.PriceRepository;
import com.flashkart.catalog.infra.SkuRepository;
import com.flashkart.order.domain.*;
import com.flashkart.order.infra.OrderItemRepository;
import com.flashkart.order.infra.OrderRepository;
import com.flashkart.shared.error.BusinessException;
import com.flashkart.shared.error.ErrorCode;
import com.flashkart.shared.error.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service orchestrating the atomic checkout flow.
 * 
 * Transaction Isolation Level: READ_COMMITTED
 * - Prevents dirty reads while allowing concurrent transactions
 * - Uses pessimistic locking on SKU entities to prevent overselling
 * - Ensures atomicity: order creation, inventory reservation, and payment intent creation
 * 
 * Rollback Rules:
 * - BusinessException (non-retryable): Rollback transaction
 * - BusinessException (retryable): Rollback transaction (client should retry with same idempotency key)
 * - RuntimeException: Rollback transaction
 * - No rollback for: checked exceptions (if any)
 */
@Service
public class CheckoutService {

    private static final Logger logger = LoggerFactory.getLogger(CheckoutService.class);

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final SkuRepository skuRepository;
    private final PriceRepository priceRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final InventoryReservationService inventoryReservationService;
    private final PaymentIntentService paymentIntentService;
    private final OrderNumberGenerator orderNumberGenerator;

    public CheckoutService(
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            SkuRepository skuRepository,
            PriceRepository priceRepository,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            InventoryReservationService inventoryReservationService,
            PaymentIntentService paymentIntentService,
            OrderNumberGenerator orderNumberGenerator) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.skuRepository = skuRepository;
        this.priceRepository = priceRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.inventoryReservationService = inventoryReservationService;
        this.paymentIntentService = paymentIntentService;
        this.orderNumberGenerator = orderNumberGenerator;
    }

    /**
     * Process checkout for a user's cart.
     * 
     * This method performs the following operations atomically:
     * 1. Validate cart and items
     * 2. Create order draft
     * 3. Reserve inventory
     * 4. Create payment intent
     * 
     * If any step fails, the entire transaction is rolled back.
     * 
     * @param userId The user ID
     * @param cartId The cart ID to checkout
     * @return The created order
     */
    @Transactional(
            isolation = Isolation.READ_COMMITTED,
            rollbackFor = {BusinessException.class, RuntimeException.class}
    )
    public Order checkout(UUID userId, UUID cartId) {
        logger.info("Starting checkout: userId={}, cartId={}", userId, cartId);

        // Step 1: Load and validate cart
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new NotFoundException("Cart not found: " + cartId));

        if (!cart.getUserId().equals(userId)) {
            throw new BusinessException(
                    ErrorCode.FORBIDDEN,
                    "Cart does not belong to user",
                    false
            );
        }

        if (cart.isExpired()) {
            throw new BusinessException(
                    ErrorCode.CART_EXPIRED,
                    "Cart has expired",
                    false
            );
        }

        if (cart.isDeleted()) {
            throw new BusinessException(
                    ErrorCode.CART_NOT_FOUND,
                    "Cart has been deleted",
                    false
            );
        }

        // Load cart items explicitly (cart.items is lazy)
        List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getId()).stream()
                .filter(item -> !item.isDeleted())
                .collect(Collectors.toList());

        if (cartItems.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "Cart is empty",
                    false
            );
        }

        // Step 2: Validate SKUs and calculate totals
        Map<UUID, Integer> skuQuantities = new HashMap<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        String currency = "USD";

        for (CartItem cartItem : cartItems) {
            UUID skuId = cartItem.getSkuId();
            Integer quantity = cartItem.getQuantity();

            // Load SKU with price
            Sku sku = skuRepository.findById(skuId)
                    .orElseThrow(() -> new BusinessException(
                            ErrorCode.SKU_NOT_FOUND,
                            "SKU not found: " + skuId,
                            false
                    ));

            if (!sku.isActive() || sku.isDeleted()) {
                throw new BusinessException(
                        ErrorCode.SKU_NOT_FOUND,
                        "SKU is not available: " + skuId,
                        false
                );
            }

            // Check availability (basic check, detailed check happens in reservation)
            if (sku.getAvailableQuantity() < quantity) {
                throw new BusinessException(
                        ErrorCode.INSUFFICIENT_STOCK,
                        String.format("Insufficient stock for SKU %s. Available: %d, Requested: %d",
                                skuId, sku.getAvailableQuantity(), quantity),
                        false
                );
            }

            // Get current price
            Price currentPrice = sku.getCurrentPrice();
            if (currentPrice == null || currentPrice.isDeleted()) {
                throw new BusinessException(
                        ErrorCode.PRICE_NOT_FOUND,
                        "Price not found for SKU: " + skuId,
                        false
                );
            }

            skuQuantities.put(skuId, quantity);
            BigDecimal itemTotal = currentPrice.getAmount().multiply(BigDecimal.valueOf(quantity));
            totalAmount = totalAmount.add(itemTotal);
        }

        // Step 3: Create order draft
        String orderNumber = orderNumberGenerator.generate();
        Order order = new Order(userId, orderNumber, totalAmount, currency, cartId);
        order.setStatus(OrderStatus.DRAFT);
        order = orderRepository.save(order);

        // Step 4: Create order items
        for (CartItem cartItem : cartItems) {
            UUID skuId = cartItem.getSkuId();
            Sku sku = skuRepository.findById(skuId).orElseThrow();
            Price currentPrice = sku.getCurrentPrice();

            OrderItem orderItem = new OrderItem(
                    order,
                    skuId,
                    cartItem.getQuantity(),
                    currentPrice.getAmount(),
                    currency
            );
            order.addItem(orderItem);
            orderItemRepository.save(orderItem);
        }

        // Step 5: Reserve inventory (this will use pessimistic locking)
        try {
            inventoryReservationService.reserveInventory(order.getId(), skuQuantities);
        } catch (BusinessException e) {
            // If inventory reservation fails, transaction will rollback
            logger.error("Inventory reservation failed for order: orderId={}", order.getId(), e);
            throw e;
        }

        // Step 6: Create payment intent
        PaymentIntent paymentIntent = paymentIntentService.createPaymentIntent(order);

        // Step 7: Transition order to PENDING_PAYMENT
        order.transitionTo(OrderStatus.PENDING_PAYMENT);
        order = orderRepository.save(order);

        logger.info("Checkout completed successfully: orderId={}, orderNumber={}, totalAmount={}",
                order.getId(), orderNumber, totalAmount);

        return order;
    }
}
