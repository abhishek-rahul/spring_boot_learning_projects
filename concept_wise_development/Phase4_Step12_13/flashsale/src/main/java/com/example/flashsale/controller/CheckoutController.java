package com.example.flashsale.controller;

import com.example.flashsale.dto.*;
import com.example.flashsale.exception.ApiResponse;
import com.example.flashsale.service.CheckoutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class CheckoutController {

    private final CheckoutService checkoutService;

    @PostMapping("/checkout")
    public ApiResponse<CheckoutResponse> checkout(@RequestBody @Valid CheckoutRequest req) throws IOException {
        return ApiResponse.ok(checkoutService.checkout(req));
    }
}
