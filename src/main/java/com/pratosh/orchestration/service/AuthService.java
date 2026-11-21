package com.pratosh.orchestration.service;

import com.pratosh.orchestration.dto.LoginRequest;
import com.pratosh.orchestration.dto.LoginResponse;
import com.pratosh.orchestration.entity.Role;
import com.pratosh.orchestration.entity.User;
import com.pratosh.orchestration.repository.UserRepository;
import com.pratosh.orchestration.security.JwtService;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Handles authentication business logic for the platform.
 *
 * This initial implementation permits administrator login only.
 * Merchant and operations authentication can be added later.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /**
     * Constructor injection allows Spring to supply the dependencies.
     */
    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * Authenticates an administrator and generates a JWT.
     *
     * @param request the username and password supplied by the client
     * @return the access token and basic account information
     */
    public LoginResponse login(LoginRequest request) {

        // Find the account using its unique username.
        // Return a generic error if the account does not exist.
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(this::invalidCredentials);

        // Reject disabled accounts and accounts that are not administrators.
        if (!user.isEnabled() || user.getRole() != Role.ADMIN) {
            throw invalidCredentials();
        }

        // Compare the supplied password with the stored BCrypt hash.
        // Never compare a plain-text password directly with the database value.
        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword()
        )) {
            throw invalidCredentials();
        }

        // Generate the JWT only after successful authentication.
        String token = jwtService.generateToken(user);

        // Return basic user information without exposing the password hash.
        return new LoginResponse(
                token,
                "Bearer",
                user.getId(),
                user.getUsername(),
                user.getRole().name(),
                jwtService.getExpirationSeconds()
        );
    }

    /**
     * Creates a generic unauthorized response.
     *
     * The same message is used for invalid credentials and accounts
     * that cannot access the administrator login.
     */
    private ResponseStatusException invalidCredentials() {
        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Invalid username or password"
        );
    }
}