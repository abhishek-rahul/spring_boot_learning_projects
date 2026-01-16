package com.example.flashsale.web;

import com.example.flashsale.gate.FlashSaleGate;
import com.example.flashsale.service.OrderService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@RestController
@RequestMapping("/loadtest")
public class LoadTestController {

    private final FlashSaleGate gate;
    private final OrderService orderService;
    private final ExecutorService orderExecutor;

    public LoadTestController(FlashSaleGate gate,
                              OrderService orderService,
                              @Qualifier("orderExecutor") ExecutorService orderExecutor) {
        this.gate = gate;
        this.orderService = orderService;
        this.orderExecutor = orderExecutor;
    }

    @PostMapping("/setup")
    public String setup(@RequestParam int users) {
        gate.createBarrier(users);
        return "Barrier created for users=" + users;
    }

    @PostMapping("/run")
    public String run(@RequestParam int users,
                      @RequestParam String sku,
                      @RequestParam int qty) throws Exception {

        List<CompletableFuture<String>> results = new ArrayList<>(users);

        // Create N tasks: all will wait on barrier and then place order simultaneously
        for (int i = 0; i < users; i++) {
            results.add(CompletableFuture.supplyAsync(() -> {
                gate.awaitStart(); // CyclicBarrier: simultaneous start
                return orderService.placeOrder(sku, qty).join();
            }, orderExecutor));
        }

        CompletableFuture.allOf(results.toArray(new CompletableFuture[0])).get(30, TimeUnit.SECONDS);

        long ok = results.stream().filter(f -> "ORDER_PLACED".equals(f.join())).count();
        long oos = results.stream().filter(f -> "OUT_OF_STOCK".equals(f.join())).count();
        long maint = results.stream().filter(f -> "MAINTENANCE_MODE".equals(f.join())).count();
        long failed = results.size() - ok - oos - maint;

        return "DONE users=" + users + " ok=" + ok + " oos=" + oos + " maint=" + maint + " other=" + failed;
    }
}
