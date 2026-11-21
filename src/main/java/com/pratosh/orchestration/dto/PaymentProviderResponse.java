package com.pratosh.orchestration.dto;

import com.pratosh.orchestration.entity.PaymentProvider;

import java.time.LocalDateTime;

/**
 * Defines the safe, public representation of a payment provider.
 *
 * This DTO prevents the API from returning the database entity directly.
 * Only explicitly selected fields are exposed to API clients.
 *
 * Provider secrets such as merchant keys and salt keys must never
 * be included in this response.
 */
public class PaymentProviderResponse {

    // Database identifier of the provider.
    private Long id;

    // Human-readable provider name, such as Razorpay or PayU.
    private String providerName;

    // Unique internal identifier, such as RAZORPAY.
    private String providerCode;

    // Configured environment: SANDBOX or PRODUCTION.
    private String environment;

    // Merchant identifier, if configured.
    private String merchantId;

    // Indicates whether the provider is enabled.
    private boolean enabled;

    // Lower number means higher routing preference.
    private Integer routingPriority;

    // Timestamp when the provider was created.
    private LocalDateTime createdAt;

    // Timestamp when the provider was last updated.
    private LocalDateTime updatedAt;

    /**
     * Converts a PaymentProvider entity into a response DTO.
     *
     * Only explicitly selected fields are returned by the API.
     */
    public static PaymentProviderResponse fromEntity(
            PaymentProvider provider) {

        PaymentProviderResponse response =
                new PaymentProviderResponse();

        response.setId(provider.getId());
        response.setProviderName(provider.getProviderName());
        response.setProviderCode(provider.getProviderCode());
        response.setEnvironment(provider.getEnvironment());
        response.setMerchantId(provider.getMerchantId());
        response.setEnabled(provider.isEnabled());
        response.setRoutingPriority(provider.getRoutingPriority());
        response.setCreatedAt(provider.getCreatedAt());
        response.setUpdatedAt(provider.getUpdatedAt());

        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public String getProviderCode() {
        return providerCode;
    }

    public void setProviderCode(String providerCode) {
        this.providerCode = providerCode;
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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getRoutingPriority() {
        return routingPriority;
    }

    public void setRoutingPriority(Integer routingPriority) {
        this.routingPriority = routingPriority;
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