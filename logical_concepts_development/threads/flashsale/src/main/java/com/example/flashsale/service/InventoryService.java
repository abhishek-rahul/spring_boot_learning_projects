package com.example.flashsale.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class InventoryService {

    private final ConcurrentHashMap<String, Integer> stock = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    public void seed(String sku, int qty) {
        stock.put(sku, qty);
    }

    public int getStock(String sku) {
        return stock.getOrDefault(sku, 0);
    }

    public boolean reserve(String sku, int qty) {
        ReentrantLock lock = locks.computeIfAbsent(sku, k -> new ReentrantLock(true)); // FAIR
        lock.lock();
        try {
            int available = stock.getOrDefault(sku, 0);
            if (available < qty) return false;
            stock.put(sku, available - qty);
            return true;
        } finally {
            lock.unlock();
        }
    }
}
