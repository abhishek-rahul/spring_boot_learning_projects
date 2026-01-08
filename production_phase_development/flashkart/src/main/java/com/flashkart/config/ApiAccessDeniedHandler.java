package com.flashkart.config;

import com.flashkart.shared.api.ApiErrorResponse;   // adjust if different
import com.flashkart.shared.error.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

import com.flashkart.shared.observability.CorrelationId;
import org.slf4j.MDC;

@Component
public class ApiAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper om;

    public ApiAccessDeniedHandler(ObjectMapper om) {
        this.om = om;
    }


    // Todo : This is a temporary solution. We need to find a better way to get the request id.
    // Since we are doing it in every class
    private String requestId() {
        String v = MDC.get(CorrelationId.MDC_KEY);
        return v == null ? "unknown" : v;
    }

    private String apiVersion(HttpServletRequest req) {
        // Simple v1 default; later we can derive from path or header
        return "v1";
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
            throws IOException {

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType("application/json");

        ApiErrorResponse body = ApiErrorResponse.of(
            apiVersion(request),
            requestId(),
            request.getRequestURI(),
            HttpStatus.FORBIDDEN.value(),
            ErrorCode.FORBIDDEN.name(),
            "You are not allowed to access this resource",
            List.of(),
            false
    );

        om.writeValue(response.getOutputStream(), body);
    }
}
