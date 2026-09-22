package com.sam.insurance.dto;

public class AuthResponse {

    private final String token;
    private final String tokenType;

    public AuthResponse(
            String token,
            String tokenType
    ) {
        this.token = token;
        this.tokenType = tokenType;
    }

    public String getToken() {
        return token;
    }

    public String getTokenType() {
        return tokenType;
    }
}