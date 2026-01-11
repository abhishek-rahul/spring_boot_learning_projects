package com.flashkart.identity.api.dto;

public class TokenResponse {
    private String accessToken;
    private String tokenType;
    private long expiresIn;


    private String refreshToken;
    private long refreshExpiresIn;

    public TokenResponse(String accessToken, long expiresIn,String refreshToken, long refreshExpiresIn) {
        this.accessToken = accessToken;
        this.tokenType = "Bearer";
        this.expiresIn = expiresIn;
        this.refreshToken = refreshToken;
        this.refreshExpiresIn = refreshExpiresIn;
    }

    public String getAccessToken() { return accessToken; }
    public String getTokenType() { return tokenType; }
    public long getExpiresIn() { return expiresIn; }
    public String getRefreshToken() { return refreshToken; }
    public long getRefreshExpiresIn() { return refreshExpiresIn; }
}
