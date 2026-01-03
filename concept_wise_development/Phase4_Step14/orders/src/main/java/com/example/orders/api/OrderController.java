package com.example.orders.api;

import com.example.orders.api.dto.CheckoutRequest;
import com.example.orders.api.dto.CheckoutResponse;
import com.example.orders.service.CheckoutService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final CheckoutService checkoutService;

    public OrderController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping("/checkout")
    public CheckoutResponse checkout(@RequestParam Long userId,
                                     @RequestBody @Valid CheckoutRequest request) {
        return checkoutService.checkout(userId, request);
    }
}
