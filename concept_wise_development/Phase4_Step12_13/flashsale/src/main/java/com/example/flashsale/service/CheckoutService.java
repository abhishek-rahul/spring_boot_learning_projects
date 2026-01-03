package com.example.flashsale.service;

import com.example.flashsale.dto.*;
import com.example.flashsale.entity.*;
import com.example.flashsale.exception.*;
import com.example.flashsale.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class CheckoutService {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;

    private final PaymentService paymentService;
    private final AuditService auditService;

    /**
     * Step 12: Transaction basics
     * - Everything inside should commit or rollback together.
     * - We'll demonstrate runtime vs checked rollback behavior.
     *
     * NOTE: By default, checked exceptions (like IOException) DO NOT trigger rollback.
     * If you want checked rollback too, change to:
     *   @Transactional(rollbackFor = Exception.class)
     */
    @Transactional
    public CheckoutResponse checkout(CheckoutRequest req) throws IOException {

        // 1) Idempotency: prevent double checkout
        IdempotencyKey key = IdempotencyKey.builder()
                .idemKey(req.idempotencyKey())
                .userId(req.userId())
                .status(IdempotencyKey.Status.IN_PROGRESS)
                .createdAt(Instant.now())
                .build();

        try {
            idempotencyKeyRepository.save(key);
        } catch (DataIntegrityViolationException e) {
            // same key already exists
            IdempotencyKey existing = idempotencyKeyRepository.findByIdemKey(req.idempotencyKey())
                    .orElseThrow(() -> new DuplicateCheckoutException("Checkout already started/processed"));
            if (existing.getOrderId() != null) {
                return new CheckoutResponse(existing.getOrderId(), "ALREADY_PROCESSED", null);
            }
            throw new DuplicateCheckoutException("Checkout already in progress");
        }

        // 2) Calculate total + reserve stock (for now use normal findById)
        BigDecimal total = BigDecimal.ZERO;

        CustomerOrder order = CustomerOrder.builder()
                .userId(req.userId())
                .status(CustomerOrder.Status.CREATED)
                .totalAmount(BigDecimal.ZERO)
                .createdAt(Instant.now())
                .build();
        order = orderRepository.save(order);

        for (CartItemRequest item : req.items()) {
            Product p = productRepository.findById(item.productId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + item.productId()));

            if (p.getStock() < item.qty()) {
                throw new OutOfStockException("Product " + p.getId() + " out of stock");
            }

            p.setStock(p.getStock() - item.qty());
            productRepository.save(p);

            total = total.add(p.getPrice().multiply(BigDecimal.valueOf(item.qty())));

            orderItemRepository.save(OrderItem.builder()
                    .order(order)
                    .product(p)
                    .qty(item.qty())
                    .unitPrice(p.getPrice())
                    .build());
        }

        order.setTotalAmount(total);
        orderRepository.save(order);

        // 3) Create payment row
        Payment payment = Payment.builder()
                .order(order)
                .status(Payment.Status.PENDING)
                .createdAt(Instant.now())
                .build();
        payment = paymentRepository.save(payment);

        // 4) Propagation demo: audit should commit even if checkout rolls back
        auditService.logCheckoutEvent("Checkout started user=" + req.userId() + ", orderId=" + order.getId());

        // 5) Simulate payment
        String gatewayRef = paymentService.charge(req.simulateRuntimeFailure(), req.simulateCheckedFailure());
        payment.setGatewayRef(gatewayRef);
        payment.setStatus(Payment.Status.SUCCESS);
        paymentRepository.save(payment);

        order.setStatus(CustomerOrder.Status.PAID);
        orderRepository.save(order);

        // 6) Mark idempotency completed
        key.setStatus(IdempotencyKey.Status.COMPLETED);
        key.setOrderId(order.getId());
        idempotencyKeyRepository.save(key);

        return new CheckoutResponse(order.getId(), order.getStatus().name(), order.getTotalAmount());
    }
}
