package com.pratosh.orchestration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Represents the login credentials received from the client.
 *
 * This DTO is used by the authentication API.
 */
public class LoginRequest {

    // The username supplied by the administrator.
    @NotBlank(message = "Username is required")
    @Size(max = 50, message = "Username cannot exceed 50 characters")
    private String username;

    // The plain-text password is accepted only for authentication.
    // It must never be logged or stored directly in the database.
    @NotBlank(message = "Password is required")
    @Size(max = 72, message = "Password cannot exceed 72 characters")
    private String password;

    // Required for JSON-to-Java object conversion.
    public LoginRequest() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}