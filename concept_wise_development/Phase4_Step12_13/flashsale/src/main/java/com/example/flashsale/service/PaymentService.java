package com.example.flashsale.service;

import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class PaymentService {

    public String charge(boolean simulateRuntimeFailure, boolean simulateCheckedFailure) throws IOException {
        if (simulateRuntimeFailure) {
            throw new RuntimeException("Simulated payment runtime failure (should rollback)");
        }
        if (simulateCheckedFailure) {
            throw new IOException("Simulated payment checked failure (default: may NOT rollback)");
        }
        return "GATEWAY_REF_" + System.currentTimeMillis();
    }
}
