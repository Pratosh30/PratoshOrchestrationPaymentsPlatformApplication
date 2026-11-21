package com.pratosh.orchestration.dto;

/**
 * Represents the response returned after successful authentication.
 *
 * Sensitive information such as the password and password hash
 * must never be included in this response.
 */
public class LoginResponse {

    // JWT access token used to authenticate subsequent API requests.
    private String accessToken;

    // Indicates the token type expected by the client.
    private String tokenType;

    // Basic information about the authenticated user.
    private Long userId;
    private String username;
    private String role;

    // Token lifetime in seconds.
    private long expiresIn;

    public LoginResponse() {
    }

    public LoginResponse(
            String accessToken,
            String tokenType,
            Long userId,
            String username,
            String role,
            long expiresIn
    ) {
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.expiresIn = expiresIn;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }
}