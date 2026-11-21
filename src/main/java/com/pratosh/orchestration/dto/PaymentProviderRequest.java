package com.pratosh.orchestration.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Represents the data an administrator submits when registering
 * a payment service provider.
 *
 * A DTO (Data Transfer Object) separates incoming API data from
 * the database entity.
 */
public class PaymentProviderRequest {

    // Human-readable provider name, such as Razorpay or PayU.
    @NotBlank(message = "Provider name is required.")
    @Size(max = 100, message = "Provider name cannot exceed 100 characters.")
    private String providerName;

    // Internal provider identifier, such as RAZORPAY or PAYU.
    @NotBlank(message = "Provider code is required.")
    @Size(max = 50, message = "Provider code cannot exceed 50 characters.")
    @Pattern(
            regexp = "^[A-Za-z0-9_-]+$",
            message = "Provider code may contain only letters, numbers, underscores, and hyphens."
    )
    private String providerCode;

    // Environment in which the provider will operate.
    // Only SANDBOX and PRODUCTION are accepted.
    @NotBlank(message = "Environment is required.")
    @Pattern(
            regexp = "(?i)^(SANDBOX|PRODUCTION)$",
            message = "Environment must be SANDBOX or PRODUCTION."
    )
    private String environment;

    // Optional merchant identifier assigned by the provider.
    @Size(max = 150, message = "Merchant ID cannot exceed 150 characters.")
    private String merchantId;

    @Min(value = 1, message = "Routing priority must be at least 1.")
    @Max(value = 10000, message = "Routing priority cannot exceed 10000.")
    private Integer routingPriority = 100;

    public Integer getRoutingPriority() {
        return routingPriority;
    }

    public void setRoutingPriority(Integer routingPriority) {
        this.routingPriority = routingPriority;
    }

    // Required by JSON deserialization frameworks.
    public PaymentProviderRequest() {
    }

    // Getters and setters allow Jackson to read incoming JSON.

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
}
