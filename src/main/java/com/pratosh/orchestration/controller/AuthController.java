package com.pratosh.orchestration.controller;

import com.pratosh.orchestration.dto.LoginRequest;
import com.pratosh.orchestration.dto.LoginResponse;
import com.pratosh.orchestration.service.AuthService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

//REST controller for authentication-related operations.
//Exposes the login endpoint to clients such as Postman and the React frontend.
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {   // Constructor injection supplies the authentication service.
        this.authService = authService;
    }
    //Authenticates an administrator and returns a JWT.  Endpoint: POST /api/v1/auth/login
    //@param request the login credentials supplied in JSON
    //@return a response containing the JWT and basic user details
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        LoginResponse response = authService.login(request); // Delegate authentication and token generation to the service.

        return ResponseEntity.ok(response); // Return HTTP 200 OK when authentication succeeds.
    }
}