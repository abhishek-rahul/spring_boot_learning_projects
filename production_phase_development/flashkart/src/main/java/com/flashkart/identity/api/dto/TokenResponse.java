package com.flashkart.identity.api.dto;

public class TokenResponse {
    private String accessToken;
    private String tokenType;
    private long expiresIn;

    public TokenResponse(String accessToken, long expiresIn) {
        this.accessToken = accessToken;
        this.tokenType = "Bearer";
        this.expiresIn = expiresIn;
    }

    public String getAccessToken() { return accessToken; }
    public String getTokenType() { return tokenType; }
    public long getExpiresIn() { return expiresIn; }
}
