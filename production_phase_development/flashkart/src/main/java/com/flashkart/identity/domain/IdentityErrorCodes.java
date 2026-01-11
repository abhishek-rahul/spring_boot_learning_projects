package com.flashkart.identity.domain;

public final class IdentityErrorCodes {
    private IdentityErrorCodes() {}

    public static final String USER_ALREADY_EXISTS = "USER_ALREADY_EXISTS";
    public static final String INVALID_CREDENTIALS = "INVALID_CREDENTIALS";
    public static final String USER_NOT_FOUND = "USER_NOT_FOUND";
    public static final String USER_BLOCKED = "USER_BLOCKED";
}
