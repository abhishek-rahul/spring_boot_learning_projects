package com.flashkart.config;

import com.flashkart.shared.api.ApiErrorResponse; // adjust if different
import com.flashkart.shared.error.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

import com.flashkart.shared.observability.CorrelationId;
import org.slf4j.MDC;

@Component
public class ApiAuthEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper om;

    public ApiAuthEntryPoint(ObjectMapper om) {
        this.om = om;
    }

    private String requestId() {
        String v = MDC.get(CorrelationId.MDC_KEY);
        return v == null ? "unknown" : v;
    }

    private String apiVersion(HttpServletRequest req) {
        // Simple v1 default; later we can derive from path or header
        return "v1";
    }

    @Override
    public void commence(HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException {

        if (response.isCommitted())
            return;

        response.resetBuffer(); // safe because not committed
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        ApiErrorResponse body = ApiErrorResponse.of(
                apiVersion(request),
                requestId(),
                request.getRequestURI(),
                HttpStatus.UNAUTHORIZED.value(),
                ErrorCode.UNAUTHORIZED.name(),
                "Missing or invalid access token",
                List.of(),
                false);

        om.writeValue(response.getOutputStream(), body);
        response.flushBuffer();
    }
}
