package com.flashkart.identity.api;

import com.flashkart.identity.api.dto.LoginRequest;
import com.flashkart.identity.api.dto.SignupRequest;
import com.flashkart.identity.api.dto.UserResponse;
import com.flashkart.identity.domain.User;
import com.flashkart.identity.service.AuthService;
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

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    public ApiResponse<UserResponse> signup(@Valid @RequestBody SignupRequest req) {
        User u = authService.signup(req.getEmail(), req.getPassword());
	String requestId = MDC.get(CorrelationId.MDC_KEY);
        return ApiResponse.ok("v1",requestId,toResponse(u));
    }

    @PostMapping("/login")
    public ApiResponse<UserResponse> login(@Valid @RequestBody LoginRequest req) {
	String requestId = MDC.get(CorrelationId.MDC_KEY);
        User u = authService.login(req.getEmail(), req.getPassword());
        return ApiResponse.ok("v1",requestId,toResponse(u));
    }

    private UserResponse toResponse(User u) {
        return new UserResponse(
                u.getId(),
                u.getEmail(),
                u.getRoles().stream().map(Enum::name).collect(Collectors.toSet())
        );
    }
}
