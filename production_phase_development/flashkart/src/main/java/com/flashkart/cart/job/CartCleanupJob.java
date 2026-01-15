package com.flashkart.cart.job;

import com.flashkart.cart.infra.CartRepository;
import com.flashkart.shared.infra.DistributedLockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * Scheduled job to clean up expired carts.
 * Uses distributed lock to ensure only one instance runs the cleanup in a distributed environment.
 */
@Component
public class CartCleanupJob {

    private static final Logger logger = LoggerFactory.getLogger(CartCleanupJob.class);
    private static final String LOCK_KEY = "cart-cleanup-job";
    private static final Duration LOCK_TIMEOUT = Duration.ofMinutes(10);

    private final CartRepository cartRepository;
    private final DistributedLockService distributedLockService;

    public CartCleanupJob(
            CartRepository cartRepository,
            DistributedLockService distributedLockService) {
        this.cartRepository = cartRepository;
        this.distributedLockService = distributedLockService;
    }

    /**
     * Clean up expired carts.
     * Runs every hour at minute 0 (e.g., 1:00, 2:00, 3:00).
     * Uses distributed lock to ensure only one instance executes.
     */
    @Scheduled(cron = "0 0 * * * ?")
    @Transactional
    public void cleanupExpiredCarts() {
        String lockToken = null;
        try {
            // Try to acquire distributed lock
            lockToken = distributedLockService.tryLock(LOCK_KEY, LOCK_TIMEOUT);
            
            if (lockToken == null) {
                logger.debug("Cart cleanup job skipped - lock already held by another instance");
                return;
            }

            logger.info("Starting cart cleanup job");
            Instant now = Instant.now();
            
            // Soft delete expired carts
            int deletedCount = cartRepository.softDeleteExpiredCarts(now, now);
            
            logger.info("Cart cleanup job completed. Deleted {} expired carts", deletedCount);
            
        } catch (Exception e) {
            logger.error("Error during cart cleanup job", e);
        } finally {
            // Always release the lock
            if (lockToken != null) {
                distributedLockService.releaseLock(LOCK_KEY, lockToken);
            }
        }
    }
}

