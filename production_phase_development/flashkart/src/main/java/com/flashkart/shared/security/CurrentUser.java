package com.flashkart.shared.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component("currentUser")
public class CurrentUser {

    public Optional<String> email() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return Optional.empty();
        Object principal = auth.getPrincipal();

        // With Resource Server JWT, principal is usually Jwt
        if (principal instanceof Jwt jwt) {
            // you may store email in "sub" or "email" claim
            String email = jwt.getClaimAsString("email");
            if (email != null) return Optional.of(email);
            return Optional.ofNullable(jwt.getSubject());
        }

        return Optional.ofNullable(auth.getName());
    }

    public Optional<UUID> userId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return Optional.empty();
        Object principal = auth.getPrincipal();

        if (principal instanceof Jwt jwt) {
            // If you include userId claim in JWT (recommended)
            //UUID userId = jwt.getClaim("userId");
            UUID userId = UUID.fromString(jwt.getClaim("userId"));
            return Optional.ofNullable(userId);
        }
        return Optional.empty();
    }
}
