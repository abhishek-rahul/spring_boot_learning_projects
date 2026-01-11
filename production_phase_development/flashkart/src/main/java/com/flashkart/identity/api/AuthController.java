package com.flashkart.identity.api;

import com.flashkart.identity.api.dto.*;
import com.flashkart.identity.domain.User;
import com.flashkart.identity.service.AuthService;
import com.flashkart.identity.service.RefreshTokenService;
import com.flashkart.shared.api.ApiResponse;
import com.flashkart.shared.error.ErrorCode;
import com.flashkart.shared.observability.CorrelationId;
import com.flashkart.shared.security.JwtClaimsExtractor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import com.flashkart.shared.error.BusinessException; 

import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import com.flashkart.shared.security.ratelimit.RequestKeyUtil;
import com.flashkart.shared.security.ratelimit.LoginThrottleService;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final LoginThrottleService loginThrottleService;
    private final AuthResponseBuilder authResponseBuilder;
    private final JwtClaimsExtractor jwtClaimsExtractor;

    public AuthController(
            AuthService authService,
            RefreshTokenService refreshTokenService,
            LoginThrottleService loginThrottleService,
            AuthResponseBuilder authResponseBuilder,
            JwtClaimsExtractor jwtClaimsExtractor
    ) {
        this.authService = authService;
        this.refreshTokenService = refreshTokenService;
        this.loginThrottleService = loginThrottleService;
        this.authResponseBuilder = authResponseBuilder;
        this.jwtClaimsExtractor = jwtClaimsExtractor;
    }

    // -------------------------
    // SIGNUP -> returns user + access+refresh tokens
    // -------------------------
    @PostMapping("/signup")
    public ApiResponse<AuthResponse> signup(@Valid @RequestBody SignupRequest req, HttpServletRequest httpReq) {
        User u = authService.signup(req.getEmail(), req.getPassword());
        AuthResponse response = authResponseBuilder.buildAuthResponse(u, httpReq);
        String requestId = MDC.get(CorrelationId.MDC_KEY);
        return ApiResponse.ok("v1", requestId, response);
    }

    // -------------------------
    // LOGIN -> returns user + access+refresh tokens
    // -------------------------
    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest req, HttpServletRequest httpReq) {
        String ip = RequestKeyUtil.clientIp(httpReq);
    
        // block check first
        loginThrottleService.checkAllowed(req.getEmail(), ip);
    
        try {
            User u = authService.login(req.getEmail(), req.getPassword());
            loginThrottleService.onSuccess(req.getEmail(), ip);
            AuthResponse response = authResponseBuilder.buildAuthResponse(u, httpReq);
            String requestId = MDC.get(CorrelationId.MDC_KEY);
            return ApiResponse.ok("v1", requestId, response);
        } catch (BusinessException ex) {
            // only count failures for auth-related failures
            if (ex.getErrorCode() == ErrorCode.INVALID_CREDENTIALS
                    || ex.getErrorCode() == ErrorCode.USER_NOT_FOUND
                    || ex.getErrorCode() == ErrorCode.USER_BLOCKED) {
                loginThrottleService.onFailure(req.getEmail(), ip);
            }
            throw ex;
        }
    }
    

    // -------------------------
    // REFRESH -> rotate refresh token + new access token
    // -------------------------
    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshRequest req, HttpServletRequest httpReq) {

        // Use new method from RefreshTokenService that handles user lookup
        var result = refreshTokenService.rotateWithUser(
                req.getRefreshToken(),
                httpReq.getRemoteAddr(),
                httpReq.getHeader("User-Agent")
        );

        // Build response using builder
        AuthResponse response = authResponseBuilder.buildRefreshResponse(
                result.user(), 
                result.token(), 
                httpReq
        );

        String requestId = MDC.get(CorrelationId.MDC_KEY);
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
        UUID userId = jwtClaimsExtractor.extractUserId(authentication);
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Invalid authentication", false);
        }

        refreshTokenService.revokeAllForUser(userId);

        String requestId = MDC.get(CorrelationId.MDC_KEY);
        return ApiResponse.ok("v1", requestId, null);
    }

}
