package com.pratosh.orchestration.dto;

import com.pratosh.orchestration.entity.ProviderCredential;

import java.time.LocalDateTime;

/**
 * Safe response DTO for payment provider credentials.
 *
 * IMPORTANT:
 * This response deliberately excludes merchantKey,
 * saltKey, and their encrypted database values.
 */
public class ProviderCredentialResponse {

    private Long id;
    private Long providerId;
    private String providerName;
    private String environment;
    private String merchantId;
    private String referenceId;
    private boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ProviderCredentialResponse() {
    }

    /**
     * Converts a ProviderCredential entity into a safe response.
     *
     * Sensitive credential fields are never copied.
     */
    public static ProviderCredentialResponse fromEntity(
            ProviderCredential credential) {

        ProviderCredentialResponse response =
                new ProviderCredentialResponse();

        response.setId(credential.getId());

        response.setProviderId(
                credential.getProvider().getId()
        );

        response.setProviderName(
                credential.getProvider().getProviderName()
        );

        response.setEnvironment(credential.getEnvironment());
        response.setMerchantId(credential.getMerchantId());
        response.setReferenceId(credential.getReferenceId());
        response.setEnabled(credential.isEnabled());
        response.setCreatedAt(credential.getCreatedAt());
        response.setUpdatedAt(credential.getUpdatedAt());

        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProviderId() {
        return providerId;
    }

    public void setProviderId(Long providerId) {
        this.providerId = providerId;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
