package com.example.flashsale.gate;

import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;

@Component
public class StartupGate {

    private final CountDownLatch readyLatch = new CountDownLatch(3); // db, cache, warmup

    public void markDbReady() { readyLatch.countDown(); }
    public void markCacheReady() { readyLatch.countDown(); }
    public void markWarmupReady() { readyLatch.countDown(); }

    public void awaitReady() {
        try {
            readyLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for startup readiness", e);
        }
    }

    public long remaining() {
        return readyLatch.getCount();
    }
}
