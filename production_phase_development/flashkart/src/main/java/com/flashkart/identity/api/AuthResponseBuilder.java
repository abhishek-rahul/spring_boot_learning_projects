package com.flashkart.identity.api;

import com.flashkart.identity.api.dto.AuthResponse;
import com.flashkart.identity.api.dto.TokenResponse;
import com.flashkart.identity.api.dto.UserResponse;
import com.flashkart.identity.domain.User;
import com.flashkart.identity.service.JwtService;
import com.flashkart.identity.service.RefreshTokenService;
import com.flashkart.identity.service.RefreshTokenService.IssuedRefreshToken;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
public class AuthResponseBuilder {

    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthResponseBuilder(JwtService jwtService, RefreshTokenService refreshTokenService) {
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    public AuthResponse buildAuthResponse(User user, HttpServletRequest httpReq) {
        // access token
        String access = jwtService.generateAccessToken(user);

        // refresh token
        IssuedRefreshToken issuedRefresh = refreshTokenService.issue(
                user.getId(),
                httpReq.getRemoteAddr(),
                httpReq.getHeader("User-Agent")
        );

        TokenResponse tokenResponse = new TokenResponse(
                access,
                jwtService.accessTokenTtlSeconds(),
                issuedRefresh.refreshToken(),
                secondsLeft(issuedRefresh.expiresAt())
        );

        return new AuthResponse(
                toResponse(user),
                tokenResponse
        );
    }

    public AuthResponse buildRefreshResponse(User user, IssuedRefreshToken newRefresh, HttpServletRequest httpReq) {
        String access = jwtService.generateAccessToken(user);

        TokenResponse tokenResponse = new TokenResponse(
                access,
                jwtService.accessTokenTtlSeconds(),
                newRefresh.refreshToken(),
                secondsLeft(newRefresh.expiresAt())
        );

        return new AuthResponse(
                toResponse(user),
                tokenResponse
        );
    }

    private long secondsLeft(Instant expiresAt) {
        long sec = Duration.between(Instant.now(), expiresAt).getSeconds();
        return Math.max(sec, 0);
    }

    private UserResponse toResponse(User u) {
        return new UserResponse(
                u.getId(),
                u.getEmail(),
                u.getRoles()
        );
    }
}
