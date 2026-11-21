package com.pratosh.orchestration.controller;

import com.pratosh.orchestration.dto.ProviderCredentialRequest;
import com.pratosh.orchestration.dto.ProviderCredentialResponse;
import com.pratosh.orchestration.service.ProviderCredentialService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing payment provider credentials.
 *
 * All endpoints are under /api/v1/admin/credentials.
 * Access must be restricted to authenticated administrators.
 */
@RestController
@RequestMapping("/api/v1/admin/credentials")
public class ProviderCredentialController {

    private final ProviderCredentialService credentialService;

    public ProviderCredentialController(
            ProviderCredentialService credentialService) {
        this.credentialService = credentialService;
    }

    /**
     * Register credentials for an existing payment provider.
     *
     * POST /api/v1/admin/credentials
     */
    @PostMapping
    public ResponseEntity<ProviderCredentialResponse> createCredential(
            @Valid @RequestBody ProviderCredentialRequest request) {

        ProviderCredentialResponse response =
                credentialService.createCredential(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * Retrieve all credential configurations.
     *
     * GET /api/v1/admin/credentials
     */
    @GetMapping
    public ResponseEntity<List<ProviderCredentialResponse>> getAllCredentials() {
        return ResponseEntity.ok(credentialService.getAllCredentials());
    }

    /**
     * Retrieve all credential configurations for a provider.
     *
     * GET /api/v1/admin/credentials/provider/{providerId}
     */
    @GetMapping("/provider/{providerId}")
    public ResponseEntity<List<ProviderCredentialResponse>>
    getCredentialsByProvider(@PathVariable Long providerId) {

        List<ProviderCredentialResponse> credentials =
                credentialService.getCredentialsByProvider(providerId);

        return ResponseEntity.ok(credentials);
    }

    /**
     * Retrieve one credential configuration.
     *
     * GET /api/v1/admin/credentials/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProviderCredentialResponse> getCredentialById(
            @PathVariable Long id) {

        ProviderCredentialResponse response =
                credentialService.getCredentialById(id);

        return ResponseEntity.ok(response);
    }

    /**
     * Rotate the secrets of a credential. Write-only: the response never
     * contains any secret.
     *
     * PUT /api/v1/admin/credentials/{id}/secrets
     */
    @PutMapping("/{id}/secrets")
    public ResponseEntity<ProviderCredentialResponse> rotateSecrets(
            @PathVariable Long id,
            @Valid @RequestBody
            com.pratosh.orchestration.dto.ProviderCredentialSecretsRequest
                    request) {

        return ResponseEntity.ok(
                credentialService.rotateSecrets(id, request));
    }

    /**
     * Enable or disable a credential configuration.
     *
     * PATCH /api/v1/admin/credentials/{id}/status?enabled=true
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ProviderCredentialResponse> updateCredentialStatus(
            @PathVariable Long id,
            @RequestParam boolean enabled) {

        ProviderCredentialResponse response =
                credentialService.updateCredentialStatus(id, enabled);

        return ResponseEntity.ok(response);
    }
}