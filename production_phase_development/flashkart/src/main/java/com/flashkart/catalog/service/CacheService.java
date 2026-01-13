package com.flashkart.catalog.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * Cache service implementing cache-aside pattern using Redis.
 * Provides methods to get, put, and delete cache entries.
 */
@Service
public class CacheService {

    private static final Logger logger = LoggerFactory.getLogger(CacheService.class);
    private static final String CACHE_PREFIX = "catalog:";
    private static final Duration DEFAULT_TTL = Duration.ofHours(1);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public CacheService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Get value from cache.
     * Returns empty if key doesn't exist or deserialization fails.
     */
    public <T> Optional<T> get(String key, Class<T> type) {
        try {
            String cacheKey = CACHE_PREFIX + key;
            String value = redisTemplate.opsForValue().get(cacheKey);
            if (value == null) {
                return Optional.empty();
            }
            T result = objectMapper.readValue(value, type);
            return Optional.of(result);
        } catch (JsonProcessingException e) {
            logger.warn("Failed to deserialize cache value for key: {}", key, e);
            return Optional.empty();
        } catch (Exception e) {
            logger.warn("Error getting cache value for key: {}", key, e);
            return Optional.empty();
        }
    }

    /**
     * Put value in cache with default TTL.
     */
    public <T> void put(String key, T value) {
        put(key, value, DEFAULT_TTL);
    }

    /**
     * Put value in cache with custom TTL.
     */
    public <T> void put(String key, T value, Duration ttl) {
        try {
            String cacheKey = CACHE_PREFIX + key;
            String jsonValue = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(cacheKey, jsonValue, ttl);
        } catch (JsonProcessingException e) {
            logger.warn("Failed to serialize cache value for key: {}", key, e);
        } catch (Exception e) {
            logger.warn("Error putting cache value for key: {}", key, e);
        }
    }

    /**
     * Delete value from cache.
     */
    public void delete(String key) {
        try {
            String cacheKey = CACHE_PREFIX + key;
            redisTemplate.delete(cacheKey);
        } catch (Exception e) {
            logger.warn("Error deleting cache value for key: {}", key, e);
        }
    }

    /**
     * Delete all cache entries matching a pattern.
     * Useful for cache invalidation.
     */
    public void deletePattern(String pattern) {
        try {
            String cachePattern = CACHE_PREFIX + pattern;
            redisTemplate.delete(redisTemplate.keys(cachePattern));
        } catch (Exception e) {
            logger.warn("Error deleting cache pattern: {}", pattern, e);
        }
    }

    /**
     * Check if cache contains a key.
     */
    public boolean exists(String key) {
        try {
            String cacheKey = CACHE_PREFIX + key;
            return Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey));
        } catch (Exception e) {
            logger.warn("Error checking cache key existence: {}", key, e);
            return false;
        }
    }
}
