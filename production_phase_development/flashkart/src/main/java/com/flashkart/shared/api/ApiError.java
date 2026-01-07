package com.flashkart.shared.api;

public class ApiError {
    public String field;
    public String message;

    public ApiError(String field, String message) {
        this.field = field;
        this.message = message;
    }
}
