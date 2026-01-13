package com.flashkart.shared.infra;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Distributed lock service using Redis.
 * Ensures only one instance of a scheduled job runs at a time in a distributed environment.
 */
@Service
public class DistributedLockService {

    private static final Logger logger = LoggerFactory.getLogger(DistributedLockService.class);
    private static final String LOCK_PREFIX = "lock:";
    private static final Duration DEFAULT_LOCK_TIMEOUT = Duration.ofMinutes(5);

    private final StringRedisTemplate redisTemplate;

    public DistributedLockService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Try to acquire a distributed lock.
     * 
     * @param lockKey The key for the lock
     * @param timeout The maximum time to hold the lock
     * @return Lock token if acquired, null otherwise
     */
    public String tryLock(String lockKey, Duration timeout) {
        String fullKey = LOCK_PREFIX + lockKey;
        String lockToken = UUID.randomUUID().toString();

        try {
            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(fullKey, lockToken, timeout);
            if (Boolean.TRUE.equals(acquired)) {
                logger.debug("Acquired lock: {}", lockKey);
                return lockToken;
            } else {
                logger.debug("Failed to acquire lock: {} (already held)", lockKey);
                return null;
            }
        } catch (Exception e) {
            logger.error("Error acquiring lock: {}", lockKey, e);
            return null;
        }
    }

    /**
     * Try to acquire a distributed lock with default timeout.
     */
    public String tryLock(String lockKey) {
        return tryLock(lockKey, DEFAULT_LOCK_TIMEOUT);
    }

    /**
     * Release a distributed lock.
     * Uses Lua script to ensure only the lock owner can release it.
     * 
     * @param lockKey The key for the lock
     * @param lockToken The token returned when the lock was acquired
     * @return true if lock was released, false otherwise
     */
    public boolean releaseLock(String lockKey, String lockToken) {
        if (lockToken == null) {
            return false;
        }

        String fullKey = LOCK_PREFIX + lockKey;

        try {
            // Lua script to atomically check and delete the lock
            String script = 
                "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                "  return redis.call('del', KEYS[1]) " +
                "else " +
                "  return 0 " +
                "end";

            DefaultRedisScript<Long> releaseScript = new DefaultRedisScript<>();
            releaseScript.setScriptText(script);
            releaseScript.setResultType(Long.class);

            Long result = redisTemplate.execute(
                releaseScript,
                List.of(fullKey),
                lockToken
            );

            boolean released = result != null && result > 0;
            if (released) {
                logger.debug("Released lock: {}", lockKey);
            } else {
                logger.debug("Failed to release lock: {} (token mismatch or already released)", lockKey);
            }
            return released;
        } catch (Exception e) {
            logger.error("Error releasing lock: {}", lockKey, e);
            return false;
        }
    }

    /**
     * Check if a lock is currently held.
     */
    public boolean isLocked(String lockKey) {
        String fullKey = LOCK_PREFIX + lockKey;
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(fullKey));
        } catch (Exception e) {
            logger.error("Error checking lock status: {}", lockKey, e);
            return false;
        }
    }
}

