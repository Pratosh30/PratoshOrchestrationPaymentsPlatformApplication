package com.pratosh.orchestration.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Provides administrative endpoints for the payment orchestration platform.
 *
 * This endpoint is intended to verify authentication and authorization.
 * Access is restricted to administrators in this controller.
 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    /**
     * Returns a simple dashboard response for authenticated administrators.
     *
     * Authentication contains the identity and authorities established
     * by Spring Security, including those populated by the JWT filter.
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard(
            Authentication authentication) {

        // Defensively verify that the caller has the ADMIN authority.
        // The same restriction should also be enforced in SecurityConfig.
        boolean isAdmin = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);

        if (!isAdmin) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("success", false);
            error.put("message", "Administrator access is required.");

            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        }

        // Build a predictable JSON response for testing in Postman.
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", "Administrator authentication successful.");
        response.put("username", authentication.getName());
        response.put("role", "ADMIN");
        response.put("service", "PratoshOrchestrationPaymentPlatform");

        return ResponseEntity.ok(response);
    }
}
