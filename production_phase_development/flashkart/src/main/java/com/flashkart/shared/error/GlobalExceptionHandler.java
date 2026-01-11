package com.flashkart.shared.error;

import com.flashkart.shared.api.ApiError;
import com.flashkart.shared.api.ApiErrorResponse;
import com.flashkart.shared.observability.CorrelationId;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
//import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private String requestId() {
        String v = MDC.get(CorrelationId.MDC_KEY);
        return v == null ? "unknown" : v;
    }

    private String apiVersion(HttpServletRequest req) {
        // Simple v1 default; later we can derive from path or header
        return "v1";
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<ApiError> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ApiError(fe.getField(), fe.getDefaultMessage()))
                .toList();

        ApiErrorResponse body = ApiErrorResponse.of(
                apiVersion(req),
                requestId(),
                req.getRequestURI(),
                HttpStatus.BAD_REQUEST.value(),
                ErrorCode.VALIDATION_ERROR.name(),
                "Validation failed",
                details,
                false
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusiness(BusinessException ex, HttpServletRequest req) {
        HttpStatus status = switch (ex.getErrorCode()) {
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT, INVENTORY_CONFLICT -> HttpStatus.CONFLICT;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case RATE_LIMITED -> HttpStatus.TOO_MANY_REQUESTS;
            case LOGIN_THROTTLED -> HttpStatus.TOO_MANY_REQUESTS; 
            default -> HttpStatus.BAD_REQUEST;
        };

        ApiErrorResponse body = ApiErrorResponse.of(
                apiVersion(req),
                requestId(),
                req.getRequestURI(),
                status.value(),
                ex.getErrorCode().name(),
                ex.getMessage(),
                List.of(),
                ex.isRetryable()
        );
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnknown(Exception ex, HttpServletRequest req) {
        ApiErrorResponse body = ApiErrorResponse.of(
                apiVersion(req),
                requestId(),
                req.getRequestURI(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                ErrorCode.INTERNAL_ERROR.name(),
                "Something went wrong",
                List.of(),
                true
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
