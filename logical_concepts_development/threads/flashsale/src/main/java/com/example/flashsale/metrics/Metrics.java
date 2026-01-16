package com.example.flashsale.metrics;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class Metrics {
    public final AtomicLong ordersPlaced = new AtomicLong();
    public final AtomicLong outOfStock = new AtomicLong();
    public final AtomicLong paymentsFailed = new AtomicLong();
    public final AtomicLong emailsQueued = new AtomicLong();
    public final AtomicLong emailQueueDropped = new AtomicLong();
}
