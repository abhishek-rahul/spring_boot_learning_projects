package com.example.flashsale.web;

import com.example.flashsale.gate.StartupGate;
import com.example.flashsale.metrics.Metrics;
import com.example.flashsale.service.CatalogService;
import com.example.flashsale.service.InventoryService;
import com.example.flashsale.service.OrderService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StartupGate startupGate;
    private final InventoryService inventory;
    private final CatalogService catalog;
    private final OrderService orderService;
    private final Metrics metrics;

    public ApiController(StartupGate startupGate,
                         InventoryService inventory,
                         CatalogService catalog,
                         OrderService orderService,
                         Metrics metrics) {
        this.startupGate = startupGate;
        this.inventory = inventory;
        this.catalog = catalog;
        this.orderService = orderService;
        this.metrics = metrics;
    }

    @PostConstruct
    public void init() {
        // simulate startup tasks
        startupGate.markDbReady();
        catalog.updateSkuInfo("SKU-1", "FlashSale Phone (read-heavy catalog)");
        startupGate.markCacheReady();

        inventory.seed("SKU-1", 100); // stock = 100
        startupGate.markWarmupReady();
    }

    @PostMapping("/orders/place")
    public CompletableFuture<String> place(@RequestParam String sku, @RequestParam int qty) {
        startupGate.awaitReady();
        return orderService.placeOrder(sku, qty);
    }

    @GetMapping("/catalog/{sku}")
    public String getCatalog(@PathVariable String sku) {
        startupGate.awaitReady();
        return catalog.getSkuInfo(sku);
    }

    @GetMapping("/inventory/{sku}")
    public int stock(@PathVariable String sku) {
        return inventory.getStock(sku);
    }

    @PostMapping("/admin/maintenance")
    public String maintenance(@RequestParam boolean on) {
        orderService.setMaintenanceMode(on);
        return "maintenance=" + on;
    }

    @GetMapping("/admin/maintenance")
    public boolean maintenanceState() {
        return orderService.isMaintenanceMode();
    }

    @GetMapping("/startup/remaining")
    public long remainingStartupSteps() {
        return startupGate.remaining();
    }

    @GetMapping("/metrics")
    public Map<String, Long> getMetrics() {
        return Map.of(
                "ordersPlaced", metrics.ordersPlaced.get(),
                "outOfStock", metrics.outOfStock.get(),
                "paymentsFailed", metrics.paymentsFailed.get(),
                "emailsQueued", metrics.emailsQueued.get(),
                "emailQueueDropped", metrics.emailQueueDropped.get()
        );
    }
}
