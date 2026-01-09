package com.flashkart.identity.api;

import com.flashkart.identity.api.dto.*;
import com.flashkart.identity.domain.User;
import com.flashkart.identity.service.AuthService;
import com.flashkart.identity.service.JwtService;
import com.flashkart.identity.service.RefreshTokenService;
import com.flashkart.identity.service.RefreshTokenService.IssuedRefreshToken;
import com.flashkart.identity.infra.UserRepository;
import com.flashkart.shared.api.ApiResponse;
import com.flashkart.shared.error.ErrorCode;
import com.flashkart.shared.observability.CorrelationId;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import com.flashkart.shared.error.BusinessException; 

import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository; // refresh flow needs user claims

    public AuthController(
            AuthService authService,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            UserRepository userRepository
    ) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.userRepository = userRepository;
    }

    // -------------------------
    // SIGNUP -> returns user + access+refresh tokens
    // -------------------------
    @PostMapping("/signup")
    public ApiResponse<AuthResponse> signup(@Valid @RequestBody SignupRequest req, HttpServletRequest httpReq) {
        User u = authService.signup(req.getEmail(), req.getPassword());
        return buildAuthResponse(u, httpReq);
    }

    // -------------------------
    // LOGIN -> returns user + access+refresh tokens
    // -------------------------
    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest req, HttpServletRequest httpReq) {
        User u = authService.login(req.getEmail(), req.getPassword());
        return buildAuthResponse(u, httpReq);
    }

    // -------------------------
    // REFRESH -> rotate refresh token + new access token
    // -------------------------
    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshRequest req, HttpServletRequest httpReq) {

        // 1) Validate old refresh + get userId
        UUID userId = refreshTokenService.getUserIdFromRefresh(req.getRefreshToken());

        // 2) Rotate refresh token (old revoked, new issued)
        IssuedRefreshToken newRefresh = refreshTokenService.rotate(
                req.getRefreshToken(),
                httpReq.getRemoteAddr(),
                httpReq.getHeader("User-Agent")
        );

        // 3) Load user to create JWT claims (email/roles)
        User u = userRepository.findById(userId).
            orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "User Not Found", false));// convert to BusinessException in your project

        // 4) New access token
        String access = jwtService.generateAccessToken(u);

        TokenResponse tokenResponse = new TokenResponse(
                access,
                jwtService.accessTokenTtlSeconds(),
                newRefresh.refreshToken(),
                secondsLeft(newRefresh.expiresAt())
        );

        String requestId = MDC.get(CorrelationId.MDC_KEY);

        AuthResponse response = new AuthResponse(
            toResponse(u),
            tokenResponse
        );
        return ApiResponse.ok("v1", requestId, response);
    }

    // -------------------------
    // LOGOUT (single device) -> revoke given refresh token
    // -------------------------
    @PostMapping("/logout")
    public ApiResponse<AuthResponse> logout(@Valid @RequestBody RefreshRequest req) {
        refreshTokenService.revoke(req.getRefreshToken());

        String requestId = MDC.get(CorrelationId.MDC_KEY);
        return ApiResponse.ok("v1", requestId, null);
    }

    // -------------------------
    // LOGOUT ALL (every device) -> revoke all refresh tokens for current user
    // Requires access JWT
    // -------------------------
    @PostMapping("/logout-all")
    public ApiResponse<AuthResponse> logoutAll(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        UUID userId = UUID.fromString(jwt.getSubject());

        refreshTokenService.revokeAllForUser(userId);

        String requestId = MDC.get(CorrelationId.MDC_KEY);
        return ApiResponse.ok("v1", requestId, null);
    }

    // -------------------------
    // Helpers
    // -------------------------
    private ApiResponse<AuthResponse> buildAuthResponse(User u, HttpServletRequest httpReq) {
        // access token
        String access = jwtService.generateAccessToken(u);

        // refresh token
        IssuedRefreshToken issuedRefresh = refreshTokenService.issue(
                u.getId(),
                httpReq.getRemoteAddr(),
                httpReq.getHeader("User-Agent")
        );

        TokenResponse tokenResponse = new TokenResponse(
                access,
                jwtService.accessTokenTtlSeconds(),
                issuedRefresh.refreshToken(),
                secondsLeft(issuedRefresh.expiresAt())
        );

        AuthResponse response = new AuthResponse(
                toResponse(u),
                tokenResponse
        );

        String requestId = MDC.get(CorrelationId.MDC_KEY);
        return ApiResponse.ok("v1", requestId, response);
    }

    private long secondsLeft(Instant expiresAt) {
        long sec = Duration.between(Instant.now(), expiresAt).getSeconds();
        return Math.max(sec, 0);
    }

    private UserResponse toResponse(User u) {
        return new UserResponse(
                u.getId(),
                u.getEmail(),
                u.getRoles().stream().map(Enum::name).collect(Collectors.toSet())
        );
    }
}
