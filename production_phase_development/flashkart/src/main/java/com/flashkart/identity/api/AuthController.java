package com.flashkart.identity.api;

import com.flashkart.identity.api.dto.LoginRequest;
import com.flashkart.identity.api.dto.TokenResponse;
import com.flashkart.shared.api.ApiResponse;
import com.flashkart.shared.observability.CorrelationId;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest req, HttpServletRequest httpReq) {
        String requestId = MDC.get(CorrelationId.MDC_KEY);
        // dummy for now
        logger.info("Login attempt for user: {}", req.getEmail());
        TokenResponse data = new TokenResponse("access-token", "refresh-token");
        return ApiResponse.ok("v1", requestId, data);
    }
}
