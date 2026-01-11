package com.flashkart.shared.security.ratelimit;

import jakarta.servlet.http.HttpServletRequest;

public final class RequestKeyUtil {
    private RequestKeyUtil() {}

    public static String clientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            // first ip is real client
            return xff.split(",")[0].trim();
        }
        String xrip = req.getHeader("X-Real-IP");
        if (xrip != null && !xrip.isBlank()) return xrip.trim();
        return req.getRemoteAddr() != null ? req.getRemoteAddr() : "unknown";
    }
}
