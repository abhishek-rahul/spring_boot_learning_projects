package com.example.flashsale.service;

import com.example.flashsale.metrics.Metrics;
import com.example.flashsale.queue.EventQueueWorker;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Service
public class OrderService {

    private final InventoryService inventory;
    private final EventQueueWorker eventWorker;
    private final Metrics metrics;
    private final ExecutorService orderExecutor;
    private final ExecutorService ioExecutor;

    private volatile boolean maintenanceMode = false; // volatile: immediate visibility across threads

    public OrderService(
            InventoryService inventory,
            EventQueueWorker eventWorker,
            Metrics metrics,
            @Qualifier("orderExecutor") ExecutorService orderExecutor,
            @Qualifier("ioExecutor") ExecutorService ioExecutor) {
        this.inventory = inventory;
        this.eventWorker = eventWorker;
        this.metrics = metrics;
        this.orderExecutor = orderExecutor;
        this.ioExecutor = ioExecutor;
    }

    public void setMaintenanceMode(boolean on) {
        this.maintenanceMode = on;
    }

    public boolean isMaintenanceMode() {
        return maintenanceMode;
    }

    public CompletableFuture<String> placeOrder(String sku, int qty) {
        if (maintenanceMode) {
            return CompletableFuture.completedFuture("MAINTENANCE_MODE");
        }

        return CompletableFuture
                .supplyAsync(() -> inventory.reserve(sku, qty), orderExecutor)
                .thenCompose(reserved -> {
                    if (!reserved) {
                        metrics.outOfStock.incrementAndGet();
                        return CompletableFuture.completedFuture("OUT_OF_STOCK"); // final String
                    }

                    // payment returns Boolean, then convert to String here itself
                    return CompletableFuture
                            .supplyAsync(this::paySimulated, ioExecutor)
                            .thenApply(paymentOk -> {
                                if (!paymentOk) {
                                    metrics.paymentsFailed.incrementAndGet();
                                    return "PAYMENT_FAILED";
                                }

                                metrics.ordersPlaced.incrementAndGet();
                                eventWorker.submit(() -> sendEmailSimulated(sku, qty));
                                return "ORDER_PLACED";
                            });
                })
                .exceptionally(ex -> "ERROR: " + ex.getClass().getSimpleName() + " " + ex.getMessage());
    }

    private boolean paySimulated() {
        try {
            Thread.sleep(80); // simulate IO latency
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private void sendEmailSimulated(String sku, int qty) {
        try {
            Thread.sleep(200); // slow external email
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
