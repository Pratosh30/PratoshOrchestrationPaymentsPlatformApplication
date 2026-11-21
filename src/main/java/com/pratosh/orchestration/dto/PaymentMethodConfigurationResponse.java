package com.pratosh.orchestration.dto;

import com.pratosh.orchestration.entity.PaymentMethodConfiguration;

import java.time.LocalDateTime;

//DTO returned by the Payment Method Configuration APIs.
//It exposes the necessary configuration details without returning the complete JPA entity to the client.

public class PaymentMethodConfigurationResponse {

    // Unique ID of this configuration.
    private Long id;

    // ID of the associated payment provider.
    private Long providerId;

    // Human-readable provider name, such as Razorpay or PayU.
    private String providerName;

    // Configured payment method, such as UPI or CREDIT_CARD.
    private String paymentMethod;

    // Whether this payment method is enabled.
    private boolean enabled;

    // Timestamp when the configuration was created.
    private LocalDateTime createdAt;

    // Timestamp when the configuration was last updated.
    private LocalDateTime updatedAt;

    // Default constructor.
    public PaymentMethodConfigurationResponse() {
    }

     // Converts a database entity into an API response DTO.
     //Keeping this mapping in one place avoids duplicating conversion logic in multiple controller methods.

    public static PaymentMethodConfigurationResponse fromEntity(
            PaymentMethodConfiguration configuration) {

        PaymentMethodConfigurationResponse response =
                new PaymentMethodConfigurationResponse();

        response.setId(configuration.getId());

        // Read the provider's ID and name from the relationship.
        response.setProviderId(
                configuration.getProvider().getId()
        );

        response.setProviderName(
                configuration.getProvider().getProviderName()
        );

        // Convert the enum to its string representation.
        response.setPaymentMethod(
                configuration.getPaymentMethod().name()
        );

        response.setEnabled(configuration.isEnabled());

        response.setCreatedAt(configuration.getCreatedAt());
        response.setUpdatedAt(configuration.getUpdatedAt());

        return response;
    }

    // Getters and setters used by Jackson to serialize the response.

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

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
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