package com.pratosh.orchestration.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Represents a payment service provider configured in the platform.
 *
 * Examples include Razorpay, PayU, and other payment providers.
 *
 * This entity stores provider configuration and operational status.
 * Sensitive credentials will be handled separately through encrypted
 * storage and will never be returned directly by API responses.
 */
@Entity
@Table(
        name = "payment_providers",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_payment_provider_code",
                        columnNames = "provider_code"
                )
        }
)
public class PaymentProvider {

    // Database-generated primary key.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Human-readable provider name, for example "Razorpay".
    @Column(name = "provider_name", nullable = false, length = 100)
    private String providerName;

    // Unique identifier used internally, for example "RAZORPAY".
    @Column(name = "provider_code", nullable = false, length = 50)
    private String providerCode;

    // Identifies the configured environment: SANDBOX or PRODUCTION.
    @Column(name = "environment", nullable = false, length = 20)
    private String environment;

    // Merchant identifier assigned by the payment provider.
    @Column(name = "merchant_id", length = 150)
    private String merchantId;

    // Indicates whether the provider is available for routing payments.
    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    // Records when the provider configuration was created.
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Records when the provider configuration was last modified.
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Automatically populate audit timestamps before inserting a record.
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    // Update the modification timestamp before updating a record.
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Column(name = "routing_priority", nullable = false)
    private Integer routingPriority = 100;
    public Integer getRoutingPriority() {
        return routingPriority;
    }

    public void setRoutingPriority(Integer routingPriority) {
        this.routingPriority = routingPriority;
    }


    // Required by JPA when loading an entity from the database.
    public PaymentProvider() {
    }

    // Getter and setter methods.

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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
