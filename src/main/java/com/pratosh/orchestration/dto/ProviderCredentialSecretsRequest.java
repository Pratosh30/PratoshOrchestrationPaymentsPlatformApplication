package com.pratosh.orchestration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Write-only request used to rotate the secrets of a credential.
 * Stored secrets are never returned by any endpoint.
 */
public class ProviderCredentialSecretsRequest {

    @NotBlank(message = "Merchant key is required")
    @Size(max = 2000, message = "Merchant key is too long")
    private String merchantKey;

    @Size(max = 2000, message = "Salt key is too long")
    private String saltKey;

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
}
