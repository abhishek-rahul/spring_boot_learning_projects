package com.flashkart.shared.security.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public class RateLimitProperties {

    private RateLimitGroup rateLimit = new RateLimitGroup();
    private LoginThrottle loginThrottle = new LoginThrottle();

    public RateLimitGroup getRateLimit() { return rateLimit; }
    public void setRateLimit(RateLimitGroup rateLimit) { this.rateLimit = rateLimit; }

    public LoginThrottle getLoginThrottle() { return loginThrottle; }
    public void setLoginThrottle(LoginThrottle loginThrottle) { this.loginThrottle = loginThrottle; }

    public static class RateLimitGroup {
        private Rule auth = new Rule();
        private Rule api = new Rule();
        private Rule admin = new Rule();

        public Rule getAuth() { return auth; }
        public void setAuth(Rule auth) { this.auth = auth; }

        public Rule getApi() { return api; }
        public void setApi(Rule api) { this.api = api; }

        public Rule getAdmin() { return admin; }
        public void setAdmin(Rule admin) { this.admin = admin; }
    }

    public static class Rule {
        private int windowSeconds = 60;
        private int maxRequests = 60;

        public int getWindowSeconds() { return windowSeconds; }
        public void setWindowSeconds(int windowSeconds) { this.windowSeconds = windowSeconds; }

        public int getMaxRequests() { return maxRequests; }
        public void setMaxRequests(int maxRequests) { this.maxRequests = maxRequests; }
    }

    public static class LoginThrottle {
        private int maxFailures = 5;
        private int failureWindowSeconds = 900;
        private int blockSeconds = 900;

        public int getMaxFailures() { return maxFailures; }
        public void setMaxFailures(int maxFailures) { this.maxFailures = maxFailures; }

        public int getFailureWindowSeconds() { return failureWindowSeconds; }
        public void setFailureWindowSeconds(int failureWindowSeconds) { this.failureWindowSeconds = failureWindowSeconds; }

        public int getBlockSeconds() { return blockSeconds; }
        public void setBlockSeconds(int blockSeconds) { this.blockSeconds = blockSeconds; }
    }
}
