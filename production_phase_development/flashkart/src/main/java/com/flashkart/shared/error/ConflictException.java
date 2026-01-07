package com.flashkart.shared.error;

public class ConflictException extends BusinessException {
    public ConflictException(ErrorCode code, String message, boolean retryable) {
        super(code, message, retryable);
    }
}