package com.flashkart.shared.api;

import java.time.Instant;
import java.util.List;

public class ApiErrorResponse {
    public boolean success = false;
    public String apiVersion;
    public String requestId;
    public Instant timestamp;
    public String path;
    public int httpStatus;

    public String errorCode;
    public String errorMessage;
    public List<ApiError> errorDetails;
    public boolean retryable;

    public static ApiErrorResponse of(
            String apiVersion,
            String requestId,
            String path,
            int httpStatus,
            String errorCode,
            String errorMessage,
            List<ApiError> details,
            boolean retryable
    ) {
        ApiErrorResponse e = new ApiErrorResponse();
        e.apiVersion = apiVersion;
        e.requestId = requestId;
        e.timestamp = Instant.now();
        e.path = path;
        e.httpStatus = httpStatus;
        e.errorCode = errorCode;
        e.errorMessage = errorMessage;
        e.errorDetails = details;
        e.retryable = retryable;
        return e;
    }
}
