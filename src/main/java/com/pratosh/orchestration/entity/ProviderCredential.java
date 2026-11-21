package com.pratosh.orchestration.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Represents a merchant credential configuration for a payment provider.

 * Sensitive values are stored in encrypted form.
 * Encryption and decryption will be handled by a separate service.
 */
@Entity
@Table(
        name = "provider_credentials",
        uniqueConstraints = {
                // A merchant reference must be unique within a provider
                // and environment.
                @UniqueConstraint(
                        name = "uk_provider_credential_reference",
                        columnNames = {"provider_id", "environment", "reference_id"}
                )
        }
)
public class ProviderCredential {

    // Unique database identifier.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Provider associated with these credentials.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "provider_id", nullable = false)
    private PaymentProvider provider;

    // Environment in which these credentials are used.
    // Expected values: SANDBOX or PRODUCTION.
    @Column(nullable = false, length = 20)
    private String environment;

    // Merchant Identification Number or merchant account identifier.
    @Column(name = "merchant_id", nullable = false, length = 150)
    private String merchantId;

    // Encrypted merchant key.
    // Never store the original secret directly in this field.
    @Column(name = "encrypted_merchant_key", nullable = false, length = 2000)
    private String encryptedMerchantKey;

    // Encrypted salt key, if required by the provider.
    // Null is allowed for providers that do not require a salt key.
    @Column(name = "encrypted_salt_key", length = 2000)
    private String encryptedSaltKey;

    // Merchant or integration reference identifier.
    @Column(name = "reference_id", nullable = false, length = 150)
    private String referenceId;

    // Indicates whether these credentials are active.
    @Column(nullable = false)
    private boolean enabled = true;

    // Timestamp when this record was created.
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Timestamp when this record was last updated.
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Automatically set timestamps before inserting the record.
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    // Update the modification timestamp before an update.
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Required by JPA.
    public ProviderCredential() {
    }

    // Getters and setters.

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PaymentProvider getProvider() {
        return provider;
    }

    public void setProvider(PaymentProvider provider) {
        this.provider = provider;
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

    public String getEncryptedMerchantKey() {
        return encryptedMerchantKey;
    }

    public void setEncryptedMerchantKey(String encryptedMerchantKey) {
        this.encryptedMerchantKey = encryptedMerchantKey;
    }

    public String getEncryptedSaltKey() {
        return encryptedSaltKey;
    }

    public void setEncryptedSaltKey(String encryptedSaltKey) {
        this.encryptedSaltKey = encryptedSaltKey;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}