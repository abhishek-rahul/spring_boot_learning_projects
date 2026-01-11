package com.flashkart.admin.api;

import com.flashkart.shared.api.ApiResponse;
import com.flashkart.shared.observability.CorrelationId;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.MDC;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminCsrfController {

    @GetMapping("/csrf")
    public ApiResponse<Map<String, String>> csrf(HttpServletRequest req) {
        CsrfToken token = (CsrfToken) req.getAttribute(CsrfToken.class.getName());

        String requestId = MDC.get(CorrelationId.MDC_KEY);

        // token null should not happen if csrf enabled, but safe guard
        if (token == null) {
            return ApiResponse.ok("v1", requestId, Map.of(
                    "headerName", "X-XSRF-TOKEN",
                    "parameterName", "_csrf",
                    "token", ""
            ));
        }

        return ApiResponse.ok("v1", requestId, Map.of(
                "headerName", token.getHeaderName(),
                "parameterName", token.getParameterName(),
                "token", token.getToken()
        ));
    }
}
