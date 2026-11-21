package com.pratosh.orchestration.service;

import com.pratosh.orchestration.dto.ProviderCredentialRequest;
import com.pratosh.orchestration.dto.ProviderCredentialResponse;
import com.pratosh.orchestration.entity.PaymentProvider;
import com.pratosh.orchestration.entity.ProviderCredential;
import com.pratosh.orchestration.repository.PaymentProviderRepository;
import com.pratosh.orchestration.repository.ProviderCredentialRepository;
import com.pratosh.orchestration.security.EncryptionService;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/*
Handles payment provider credential management.
 Responsibilities:
 1. Validate provider and credential configuration.
 2. Encrypt secrets before storing them.
 3. Save credential records in MySQL.
 4. Return responses that never expose secrets.
 */
@Service
@Transactional(readOnly = true)
public class ProviderCredentialService {

    private final ProviderCredentialRepository credentialRepository;
    private final PaymentProviderRepository providerRepository;
    private final EncryptionService encryptionService;

    public ProviderCredentialService(
            ProviderCredentialRepository credentialRepository,
            PaymentProviderRepository providerRepository,
            EncryptionService encryptionService) {

        this.credentialRepository = credentialRepository;
        this.providerRepository = providerRepository;
        this.encryptionService = encryptionService;
    }

    /**
     * Registers a credential configuration for a payment provider.
     */
    @Transactional
    public ProviderCredentialResponse createCredential(
            ProviderCredentialRequest request) {

        // Basic defensive validation.
        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Credential request is required"
            );
        }

        if (request.getProviderId() == null
                || request.getProviderId() <= 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A valid provider ID is required"
            );
        }

        // Normalize the environment to a consistent format.
        String environment = normalizeEnvironment(
                request.getEnvironment()
        );

        String merchantId = requireText(
                request.getMerchantId(),
                "Merchant ID is required"
        );

        String merchantKey = requireText(
                request.getMerchantKey(),
                "Merchant key is required"
        );

        String referenceId = requireText(
                request.getReferenceId(),
                "Reference ID is required"
        );

        String saltKey = request.getSaltKey();

        if (saltKey != null && saltKey.isBlank()) {
            saltKey = null;
        }

        // Ensure the provider exists before saving credentials.
        PaymentProvider provider = providerRepository
                .findById(request.getProviderId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Payment provider not found"
                ));

        // Do not allow credentials to be registered for
        // a disabled provider.
        if (!provider.isEnabled()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot add credentials to a disabled provider"
            );
        }

        // Ensure the reference ID is not already used for
        // this provider and environment.
        boolean duplicate = credentialRepository
                .existsByProvider_IdAndEnvironmentAndReferenceId(
                        provider.getId(),
                        environment,
                        referenceId
                );

        if (duplicate) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A credential configuration with this reference ID already exists"
            );
        }

        // Create the entity and associate it with the provider.
        ProviderCredential credential = new ProviderCredential();

        credential.setProvider(provider);
        credential.setEnvironment(environment);
        credential.setMerchantId(merchantId);
        credential.setReferenceId(referenceId);
        credential.setEnabled(true);

        /*
         * CRITICAL SECURITY STEP:
         * Encrypt secrets before writing them to the database.
         * The entity must never receive plaintext secret values.
         */
        credential.setEncryptedMerchantKey(
                encryptionService.encrypt(merchantKey)
        );

        credential.setEncryptedSaltKey(
                saltKey == null
                        ? null
                        : encryptionService.encrypt(saltKey)
        );

        // Persist the entity and return only safe fields.
        ProviderCredential savedCredential =
                credentialRepository.save(credential);

        return ProviderCredentialResponse.fromEntity(
                savedCredential
        );
    }

    /**
     * Retrieves credential configurations for a provider.
     *
     * Secret values are excluded from every response.
     */
    public List<ProviderCredentialResponse> getCredentialsByProvider(
            Long providerId) {

        if (providerId == null || providerId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A valid provider ID is required"
            );
        }

        if (!providerRepository.existsById(providerId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Payment provider not found"
            );
        }

        return credentialRepository.findByProvider_Id(providerId)
                .stream()
                .map(ProviderCredentialResponse::fromEntity)
                .toList();
    }

    /**
     * Replaces the stored secrets of a credential (rotation).
     *
     * The new values are encrypted before storage. Old values are
     * overwritten and are never returned. A null or blank salt key
     * removes the stored salt key.
     */
    @Transactional
    public ProviderCredentialResponse rotateSecrets(
            Long id,
            com.pratosh.orchestration.dto.ProviderCredentialSecretsRequest
                    request) {

        if (id == null || id <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A valid credential ID is required"
            );
        }

        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Request body is required"
            );
        }

        String merchantKey = requireText(
                request.getMerchantKey(),
                "Merchant key is required"
        );

        String saltKey = request.getSaltKey();

        if (saltKey != null && saltKey.isBlank()) {
            saltKey = null;
        }

        ProviderCredential credential = credentialRepository
                .findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Credential configuration not found"
                ));

        credential.setEncryptedMerchantKey(
                encryptionService.encrypt(merchantKey)
        );

        credential.setEncryptedSaltKey(
                saltKey == null
                        ? null
                        : encryptionService.encrypt(saltKey)
        );

        return ProviderCredentialResponse.fromEntity(
                credentialRepository.save(credential)
        );
    }

    /**
     * Retrieves every credential configuration.
     *
     * Secret values are excluded from every response.
     */
    @Transactional(readOnly = true)
    public List<ProviderCredentialResponse> getAllCredentials() {
        return credentialRepository.findAll()
                .stream()
                .map(ProviderCredentialResponse::fromEntity)
                .toList();
    }

    /**
     * Retrieves one credential configuration by its database ID.
     */
    public ProviderCredentialResponse getCredentialById(Long id) {

        if (id == null || id <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A valid credential ID is required"
            );
        }

        ProviderCredential credential = credentialRepository
                .findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Credential configuration not found"
                ));

        return ProviderCredentialResponse.fromEntity(credential);
    }

    /**
     * Enables or disables a credential configuration.
     */
    @Transactional
    public ProviderCredentialResponse updateCredentialStatus(
            Long id,
            boolean enabled) {

        ProviderCredential credential = credentialRepository
                .findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Credential configuration not found"
                ));

        // Do not enable credentials when the provider is disabled.
        if (enabled && !credential.getProvider().isEnabled()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Enable the payment provider before enabling its credentials"
            );
        }

        credential.setEnabled(enabled);

        ProviderCredential updatedCredential =
                credentialRepository.save(credential);

        return ProviderCredentialResponse.fromEntity(
                updatedCredential
        );
    }

    /**
     * Validates and normalizes the environment.
     */
    private String normalizeEnvironment(String environment) {

        if (environment == null || environment.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Environment is required"
            );
        }

        String normalized = environment
                .trim()
                .toUpperCase(Locale.ROOT);

        if (!normalized.equals("SANDBOX")
                && !normalized.equals("PRODUCTION")) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Environment must be SANDBOX or PRODUCTION"
            );
        }

        return normalized;
    }

    /**
     * Ensures a required string is present.
     */
    private String requireText(String value, String message) {

        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    message
            );
        }

        return value.trim();
    }
}
