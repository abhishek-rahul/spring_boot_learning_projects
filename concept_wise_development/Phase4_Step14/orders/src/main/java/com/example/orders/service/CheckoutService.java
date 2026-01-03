package com.example.orders.service;

import com.example.orders.api.dto.CheckoutRequest;
import com.example.orders.api.dto.CheckoutResponse;
import com.example.orders.domain.*;
import com.example.orders.repo.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class CheckoutService {

    private final ProductRepo productRepo;
    private final InventoryRepo inventoryRepo;
    private final OrderRepo orderRepo;
    private final OrderItemRepo orderItemRepo;
    private final PaymentRepo paymentRepo;

    public CheckoutService(ProductRepo productRepo,
                           InventoryRepo inventoryRepo,
                           OrderRepo orderRepo,
                           OrderItemRepo orderItemRepo,
                           PaymentRepo paymentRepo) {
        this.productRepo = productRepo;
        this.inventoryRepo = inventoryRepo;
        this.orderRepo = orderRepo;
        this.orderItemRepo = orderItemRepo;
        this.paymentRepo = paymentRepo;
    }

    @Transactional
    public CheckoutResponse checkout(Long userId, CheckoutRequest req) {

        // 1) Create order
        Order order = new Order();
        order.setUserId(userId);
        order.setStatus("CREATED");
        order.setTotalPaise(0);
        order = orderRepo.save(order);

        long total = 0;

        // 2) For each item: validate, reduce inventory, create order_items
        for (CheckoutRequest.Item it : req.getItems()) {
            Product p = productRepo.findBySku(it.getSku())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid SKU: " + it.getSku()));

            Inventory inv = inventoryRepo.findById(p.getId())
                    .orElseThrow(() -> new IllegalStateException("Inventory missing for SKU: " + it.getSku()));

            if (inv.getAvailableQty() < it.getQty()) {
                throw new IllegalStateException("Insufficient stock for SKU: " + it.getSku());
            }

            inv.setAvailableQty(inv.getAvailableQty() - it.getQty());
            inventoryRepo.save(inv);

            OrderItem oi = new OrderItem();
            oi.setOrder(order);
            oi.setProduct(p);
            oi.setQty(it.getQty());
            oi.setPricePaise(p.getPricePaise());
            orderItemRepo.save(oi);

            total += (p.getPricePaise() * (long) it.getQty());
        }

        // 3) Create payment record (PENDING)
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setStatus("PENDING");
        payment.setProvider("DUMMY");
        paymentRepo.save(payment);

        // 4) Update total
        order.setTotalPaise(total);
        orderRepo.save(order);

        return new CheckoutResponse(order.getId(), order.getStatus(), order.getTotalPaise());
    }
}
