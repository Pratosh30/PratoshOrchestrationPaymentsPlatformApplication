package com.pratosh.orchestration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for registering payment provider credentials.
 *
 * This class validates incoming request data before
 * the application processes the credentials.
 */
public class ProviderCredentialRequest {

    @NotNull(message = "Provider ID is required")
    private Long providerId;

    @NotBlank(message = "Environment is required")
    @Pattern(
            regexp = "(?i)SANDBOX|PRODUCTION",
            message = "Environment must be SANDBOX or PRODUCTION"
    )
    private String environment;

    @NotBlank(message = "Merchant ID is required")
    @Size(max = 150, message = "Merchant ID cannot exceed 150 characters")
    private String merchantId;

    @NotBlank(message = "Merchant key is required")
    @Size(max = 2000, message = "Merchant key is too long")
    private String merchantKey;

    @Size(max = 2000, message = "Salt key is too long")
    private String saltKey;

    @NotBlank(message = "Reference ID is required")
    @Size(max = 150, message = "Reference ID cannot exceed 150 characters")
    private String referenceId;

    public ProviderCredentialRequest() {
    }

    public Long getProviderId() {
        return providerId;
    }

    public void setProviderId(Long providerId) {
        this.providerId = providerId;
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

    public String getMerchantKey() {
        return merchantKey;
    }

    public void setMerchantKey(String merchantKey) {
        this.merchantKey = merchantKey;
    }

    public String getSaltKey() {
        return saltKey;
    }

    public void setSaltKey(String saltKey) {
        this.saltKey = saltKey;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }
}
