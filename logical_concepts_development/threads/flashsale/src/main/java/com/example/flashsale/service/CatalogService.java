package com.example.flashsale.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
public class CatalogService {

    private final Map<String, String> cache = new HashMap<>();
    private final ReadWriteLock rw = new ReentrantReadWriteLock(true); // fair-ish order

    public String getSkuInfo(String sku) {
        rw.readLock().lock();
        try {
            return cache.getOrDefault(sku, "UNKNOWN_SKU");
        } finally {
            rw.readLock().unlock();
        }
    }

    public void updateSkuInfo(String sku, String info) {
        rw.writeLock().lock();
        try {
            cache.put(sku, info);
        } finally {
            rw.writeLock().unlock();
        }
    }
}
