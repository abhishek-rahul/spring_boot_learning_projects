package com.flashkart.order.service;

import com.flashkart.order.domain.Order;
import com.flashkart.order.domain.PaymentIntent;
import com.flashkart.order.domain.PaymentIntentStatus;
import com.flashkart.order.infra.PaymentIntentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Service for managing payment intents.
 * Creates payment intents for orders (actual payment gateway integration will be in Phase 9).
 */
@Service
public class PaymentIntentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentIntentService.class);

    private final PaymentIntentRepository paymentIntentRepository;

    public PaymentIntentService(PaymentIntentRepository paymentIntentRepository) {
        this.paymentIntentRepository = paymentIntentRepository;
    }

    /**
     * Create a payment intent for an order.
     * In Phase 7, this creates the intent record. Actual gateway integration will be in Phase 9.
     */
    @Transactional
    public PaymentIntent createPaymentIntent(Order order) {
        PaymentIntent paymentIntent = new PaymentIntent(order, order.getTotalAmount(), order.getCurrency());
        paymentIntent.setStatus(PaymentIntentStatus.CREATED);
        paymentIntent.setPaymentProvider("INTERNAL"); // Placeholder for now
        
        PaymentIntent saved = paymentIntentRepository.save(paymentIntent);
        order.setPaymentIntent(saved);
        
        logger.info("Created payment intent for order: orderId={}, amount={}, currency={}", 
                order.getId(), order.getTotalAmount(), order.getCurrency());
        
        return saved;
    }

    /**
     * Get payment intent for an order.
     */
    @Transactional(readOnly = true)
    public PaymentIntent getPaymentIntent(UUID orderId) {
        return paymentIntentRepository.findByOrderId(orderId)
                .orElse(null);
    }
}
