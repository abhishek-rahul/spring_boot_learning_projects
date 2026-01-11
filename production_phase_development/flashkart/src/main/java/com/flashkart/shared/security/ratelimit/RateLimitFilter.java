package com.flashkart.shared.security.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashkart.shared.api.ApiErrorResponse;
import com.flashkart.shared.error.BusinessException;
import com.flashkart.shared.security.ratelimit.RateLimitProperties;
import com.flashkart.shared.error.ErrorCode;
import com.flashkart.shared.observability.CorrelationId;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.MDC;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.util.List;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;
    private final RateLimitProperties props;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(RateLimitService rateLimitService, RateLimitProperties props, ObjectMapper objectMapper) {
        this.rateLimitService = rateLimitService;
        this.props = props;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        try {

            String path = req.getRequestURI();
            String ip = RequestKeyUtil.clientIp(req);

            // 1) Auth endpoints (public)
            if (path.startsWith("/api/v1/auth/")) {
                var rule = props.getRateLimit().getAuth();
                rateLimitService.check("auth:ip", ip, rule.getWindowSeconds(), rule.getMaxRequests());
            }

            // 2) Admin endpoints (session-based)
            else if (path.startsWith("/api/v1/admin/")) {
                var rule = props.getRateLimit().getAdmin();
                rateLimitService.check("admin:ip", ip, rule.getWindowSeconds(), rule.getMaxRequests());
            }

            // 3) General API endpoints (prefer userId -> else ip)
            else if (path.startsWith("/api/")) {
                var rule = props.getRateLimit().getApi();
                String key = resolveUserKeyOrIp(ip);
                rateLimitService.check("api:userOrIp", key, rule.getWindowSeconds(), rule.getMaxRequests());
            }

            chain.doFilter(req, res);
        } catch (BusinessException ex) {
            // ✅ Convert to uniform JSON here (so no servlet ERROR logs)
            writeBusinessError(ex, req, res);
        }

    }

    private String resolveUserKeyOrIp(String ip) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            Object principal = auth.getPrincipal();
            if (principal instanceof Jwt jwt) {
                Long userId = jwt.getClaim("userId");
                if (userId != null)
                    return "u:" + userId;
                String sub = jwt.getSubject();
                if (sub != null)
                    return "s:" + sub;
            }
            if (auth.getName() != null)
                return "n:" + auth.getName();
        }
        return "ip:" + ip;
    }

    private void writeBusinessError(BusinessException ex, HttpServletRequest req, HttpServletResponse res)
            throws IOException {

        int status = switch (ex.getErrorCode()) {
            case RATE_LIMITED, LOGIN_THROTTLED -> 429;
            case UNAUTHORIZED -> 401;
            case FORBIDDEN -> 403;
            case NOT_FOUND -> 404;
            case CONFLICT, INVENTORY_CONFLICT -> 409;
            default -> 400;
        };

        String requestId = MDC.get(CorrelationId.MDC_KEY);
        if (requestId == null)
            requestId = "unknown";

        ApiErrorResponse body = ApiErrorResponse.of(
                "v1",
                requestId,
                req.getRequestURI(),
                status,
                ex.getErrorCode().name(),
                ex.getMessage(),
                List.of(),
                ex.isRetryable());

        res.setStatus(status);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(res.getOutputStream(), body);
    }
}
