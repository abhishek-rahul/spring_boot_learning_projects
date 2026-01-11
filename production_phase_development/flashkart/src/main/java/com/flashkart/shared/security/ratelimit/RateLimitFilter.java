package com.flashkart.shared.security.ratelimit;

import com.flashkart.shared.security.ratelimit.RateLimitProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;
    private final RateLimitProperties props;

    public RateLimitFilter(RateLimitService rateLimitService, RateLimitProperties props) {
        this.rateLimitService = rateLimitService;
        this.props = props;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String path = req.getRequestURI();
        String ip = RequestKeyUtil.clientIp(req);

        // 1) Auth endpoints (public)
        if (path.startsWith("/api/v1/auth/")) {
            var rule = props.getRateLimit().getAuth();
            rateLimitService.check("auth:ip", ip, rule.getWindowSeconds(), rule.getMaxRequests());
        }

        // 2) Admin endpoints (session-based)
        if (path.startsWith("/api/v1/admin/")) {
            var rule = props.getRateLimit().getAdmin();
            rateLimitService.check("admin:ip", ip, rule.getWindowSeconds(), rule.getMaxRequests());
        }

        // 3) General API endpoints (prefer userId -> else ip)
        if (path.startsWith("/api/")) {
            var rule = props.getRateLimit().getApi();
            String key = resolveUserKeyOrIp(ip);
            rateLimitService.check("api:userOrIp", key, rule.getWindowSeconds(), rule.getMaxRequests());
        }

        chain.doFilter(req, res);
    }

    private String resolveUserKeyOrIp(String ip) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            Object principal = auth.getPrincipal();
            if (principal instanceof Jwt jwt) {
                Long userId = jwt.getClaim("userId");
                if (userId != null) return "u:" + userId;
                String sub = jwt.getSubject();
                if (sub != null) return "s:" + sub;
            }
            if (auth.getName() != null) return "n:" + auth.getName();
        }
        return "ip:" + ip;
    }
}
