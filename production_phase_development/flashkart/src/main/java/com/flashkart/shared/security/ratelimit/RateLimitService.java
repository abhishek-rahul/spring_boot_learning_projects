package com.flashkart.shared.security.ratelimit;

import com.flashkart.shared.error.BusinessException;
import com.flashkart.shared.error.ErrorCode;
import org.springframework.stereotype.Service;

@Service
public class RateLimitService {

    private final RedisAtomicCounter counter;

    public RateLimitService(RedisAtomicCounter counter) {
        this.counter = counter;
    }

    public void check(String scope, String key, int windowSeconds, int maxRequests) {
        String redisKey = "rl:" + scope + ":" + key;
        long c = counter.incrementWithWindow(redisKey, windowSeconds);

        if (c > maxRequests) {
            throw new BusinessException(
                ErrorCode.RATE_LIMITED,
                "Too many requests. Please try again later.",
                true
            );
        }
    }
}
