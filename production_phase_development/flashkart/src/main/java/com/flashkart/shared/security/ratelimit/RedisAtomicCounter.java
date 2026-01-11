package com.flashkart.shared.security.ratelimit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RedisAtomicCounter {

    private final StringRedisTemplate redis;
    private final DefaultRedisScript<Long> incrWithExpireScript;

    public RedisAtomicCounter(StringRedisTemplate redis) {
        this.redis = redis;
        this.incrWithExpireScript = new DefaultRedisScript<>();
        this.incrWithExpireScript.setResultType(Long.class);

        // Atomic:
        // count = INCR key
        // if count == 1 then EXPIRE key windowSec end
        // return count
        this.incrWithExpireScript.setScriptText(
            "local c = redis.call('INCR', KEYS[1]); " +
            "if c == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]); end; " +
            "return c;"
        );
    }

    public long incrementWithWindow(String key, int windowSeconds) {
        Long v = redis.execute(incrWithExpireScript, List.of(key), String.valueOf(windowSeconds));
        return v == null ? 0L : v;
    }

    public void delete(String key) {
        redis.delete(key);
    }

    public Boolean hasKey(String key) {
        return redis.hasKey(key);
    }

    public void setBlock(String key, int seconds) {
        redis.opsForValue().set(key, "1", java.time.Duration.ofSeconds(seconds));
    }
}
