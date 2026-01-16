package com.example.flashsale.gate;

import org.springframework.stereotype.Component;

import java.util.concurrent.CyclicBarrier;

@Component
public class FlashSaleGate {
    private volatile CyclicBarrier barrier;

    public void createBarrier(int parties) {
        this.barrier = new CyclicBarrier(parties, () -> System.out.println("🔥 FLASH SALE STARTED (barrier tripped)!"));
    }

    public void awaitStart() {
        CyclicBarrier b = this.barrier;
        if (b == null) throw new IllegalStateException("Barrier not created. Call /loadtest/setup first.");
        try {
            b.await();
        } catch (Exception e) {
            throw new RuntimeException("Barrier await failed", e);
        }
    }
}
