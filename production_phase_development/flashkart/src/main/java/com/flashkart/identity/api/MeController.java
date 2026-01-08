package com.flashkart.identity.api;

import com.flashkart.shared.api.ApiResponse; // adjust
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import org.slf4j.MDC;

import java.util.Map;
import com.flashkart.shared.observability.CorrelationId;

@RestController
@RequestMapping("/api/v1")
public class MeController {

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me(Authentication auth) {
        Jwt jwt = (Jwt) auth.getPrincipal();
        String requestId = MDC.get(CorrelationId.MDC_KEY);

        Map<String, Object> data = Map.of(
                "userId", jwt.getSubject(),
                "email", jwt.getClaimAsString("email"),
                "roles", jwt.getClaimAsString("roles")
        );

        return ApiResponse.ok("v1", requestId, data);
    }
}
