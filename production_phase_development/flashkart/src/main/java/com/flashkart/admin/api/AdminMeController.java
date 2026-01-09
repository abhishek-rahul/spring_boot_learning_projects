package com.flashkart.admin.api;

import com.flashkart.shared.api.ApiResponse;
import com.flashkart.shared.observability.CorrelationId;

import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminMeController {

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me(Authentication auth) {
        String requestId = MDC.get(CorrelationId.MDC_KEY);

        return ApiResponse.ok("v1", requestId, Map.of(
                "authenticated", auth != null && auth.isAuthenticated(),
                "user", auth != null ? auth.getName() : null,
                "authorities", auth != null ? auth.getAuthorities() : null
        ));
    }
}
