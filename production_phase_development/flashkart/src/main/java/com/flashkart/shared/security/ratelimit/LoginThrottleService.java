package com.flashkart.shared.security.ratelimit;

import com.flashkart.shared.error.BusinessException;
import com.flashkart.shared.error.ErrorCode;
import org.springframework.stereotype.Service;

@Service
public class LoginThrottleService {

    private final RedisAtomicCounter counter;
    private final RateLimitProperties props;

    public LoginThrottleService(RedisAtomicCounter counter, RateLimitProperties props) {
        this.counter = counter;
        this.props = props;
    }

    public void checkAllowed(String email, String ip) {
        String blockKey = blockKey(email, ip);
        Boolean blocked = counter.hasKey(blockKey);
        if (Boolean.TRUE.equals(blocked)) {
            throw new BusinessException(
                ErrorCode.LOGIN_THROTTLED,
                "Too many failed login attempts. Please try again later.",
                true
            );
        }
    }

    public void onFailure(String email, String ip) {
        var cfg = props.getLoginThrottle();

        String failKey = failKey(email, ip);
        long failures = counter.incrementWithWindow(failKey, cfg.getFailureWindowSeconds());

        if (failures >= cfg.getMaxFailures()) {
            counter.setBlock(blockKey(email, ip), cfg.getBlockSeconds());
        }
    }

    public void onSuccess(String email, String ip) {
        counter.delete(failKey(email, ip));
        counter.delete(blockKey(email, ip));
    }

    private String failKey(String email, String ip) {
        return "login:fail:" + normalize(email) + ":" + ip;
    }

    private String blockKey(String email, String ip) {
        return "login:block:" + normalize(email) + ":" + ip;
    }

    private String normalize(String email) {
        return email == null ? "null" : email.trim().toLowerCase();
    }
}
