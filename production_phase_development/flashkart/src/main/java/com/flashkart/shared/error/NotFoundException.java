package com.flashkart.shared.error;

public class NotFoundException extends BusinessException {
    public NotFoundException(String message) {
        super(ErrorCode.NOT_FOUND, message, false);
    }
}