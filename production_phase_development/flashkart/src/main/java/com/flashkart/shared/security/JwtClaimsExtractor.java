package com.flashkart.shared.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class JwtClaimsExtractor {

    public UUID extractUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt) {
            Object userIdClaim = jwt.getClaim("userId");
            if (userIdClaim != null) {
                if (userIdClaim instanceof UUID) {
                    return (UUID) userIdClaim;
                }
                if (userIdClaim instanceof String) {
                    return UUID.fromString((String) userIdClaim);
                }
            }
            String sub = jwt.getSubject();
            if (sub != null) {
                return UUID.fromString(sub);
            }
        }
        return null;
    }
}
