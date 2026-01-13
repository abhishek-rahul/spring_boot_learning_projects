package com.flashkart.cart.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Cache service for cart data using cache-aside pattern.
 * Caches cart reads to reduce database load.
 */
@Service
public class CartCacheService {

    private static final Logger logger = LoggerFactory.getLogger(CartCacheService.class);
    private static final String CACHE_PREFIX = "cart:";
    private static final Duration DEFAULT_TTL = Duration.ofHours(1);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public CartCacheService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Get cart from cache.
     */
    public <T> Optional<T> getCart(UUID userId, Class<T> type) {
        try {
            String cacheKey = CACHE_PREFIX + userId;
            String value = redisTemplate.opsForValue().get(cacheKey);
            if (value == null) {
                return Optional.empty();
            }
            T result = objectMapper.readValue(value, type);
            return Optional.of(result);
        } catch (JsonProcessingException e) {
            logger.warn("Failed to deserialize cart cache for user: {}", userId, e);
            return Optional.empty();
        } catch (Exception e) {
            logger.warn("Error getting cart cache for user: {}", userId, e);
            return Optional.empty();
        }
    }

    /**
     * Put cart in cache with default TTL.
     */
    public <T> void putCart(UUID userId, T cart) {
        putCart(userId, cart, DEFAULT_TTL);
    }

    /**
     * Put cart in cache with custom TTL.
     */
    public <T> void putCart(UUID userId, T cart, Duration ttl) {
        try {
            String cacheKey = CACHE_PREFIX + userId;
            String jsonValue = objectMapper.writeValueAsString(cart);
            redisTemplate.opsForValue().set(cacheKey, jsonValue, ttl);
        } catch (JsonProcessingException e) {
            logger.warn("Failed to serialize cart cache for user: {}", userId, e);
        } catch (Exception e) {
            logger.warn("Error putting cart cache for user: {}", userId, e);
        }
    }

    /**
     * Invalidate cart cache for a user.
     */
    public void invalidateCart(UUID userId) {
        try {
            String cacheKey = CACHE_PREFIX + userId;
            redisTemplate.delete(cacheKey);
            logger.debug("Invalidated cart cache for user: {}", userId);
        } catch (Exception e) {
            logger.warn("Error invalidating cart cache for user: {}", userId, e);
        }
    }

    /**
     * Check if cart exists in cache.
     */
    public boolean exists(UUID userId) {
        try {
            String cacheKey = CACHE_PREFIX + userId;
            return Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey));
        } catch (Exception e) {
            logger.warn("Error checking cart cache existence for user: {}", userId, e);
            return false;
        }
    }
}

