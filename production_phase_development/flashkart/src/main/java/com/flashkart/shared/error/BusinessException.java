package com.flashkart.shared.error;

public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;
    private final boolean retryable;

    public BusinessException(ErrorCode errorCode, String message, boolean retryable) {
        super(message);
        this.errorCode = errorCode;
        this.retryable = retryable;
    }

    public ErrorCode getErrorCode() { return errorCode; }
    public boolean isRetryable() { return retryable; }
}
