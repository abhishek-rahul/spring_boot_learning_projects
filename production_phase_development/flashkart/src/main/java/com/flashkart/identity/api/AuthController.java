package com.flashkart.identity.api;

import com.flashkart.identity.api.dto.LoginRequest;
import com.flashkart.identity.api.dto.SignupRequest;
import com.flashkart.identity.api.dto.UserResponse;
import com.flashkart.identity.api.dto.AuthResponse;
import com.flashkart.identity.api.dto.TokenResponse;
import com.flashkart.identity.domain.User;
import com.flashkart.identity.service.AuthService;
import com.flashkart.identity.service.JwtService;
import com.flashkart.shared.api.ApiResponse; // adjust if your ApiResponse path differs
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import com.flashkart.shared.observability.CorrelationId;

import java.util.stream.Collectors;
import org.slf4j.MDC;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    public AuthController(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    @PostMapping("/signup")
    public ApiResponse<AuthResponse> signup(@Valid @RequestBody SignupRequest req) {
        User u = authService.signup(req.getEmail(), req.getPassword());
        String token = jwtService.generateAccessToken(u);
        TokenResponse tokenResponse = new TokenResponse(token, jwtService.accessTokenTtlSeconds());
        
        String requestId = MDC.get(CorrelationId.MDC_KEY);
        
        AuthResponse response = new AuthResponse(
            toResponse(u),
            tokenResponse
        );
        return ApiResponse.ok("v1", requestId, response);
    }
    
    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        User u = authService.login(req.getEmail(), req.getPassword());
        String token = jwtService.generateAccessToken(u);
        TokenResponse tokenResponse = new TokenResponse(token, jwtService.accessTokenTtlSeconds());
        
        String requestId = MDC.get(CorrelationId.MDC_KEY);
        
        AuthResponse response = new AuthResponse(
            toResponse(u),
            tokenResponse
        );
        return ApiResponse.ok("v1", requestId, response);
    
    }
    

    private UserResponse toResponse(User u) {
        return new UserResponse(
                u.getId(),
                u.getEmail(),
                u.getRoles().stream().map(Enum::name).collect(Collectors.toSet())
        );
    }
}
