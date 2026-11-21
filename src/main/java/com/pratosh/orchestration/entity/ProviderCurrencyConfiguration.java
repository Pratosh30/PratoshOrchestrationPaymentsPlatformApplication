package com.pratosh.orchestration.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "provider_currency_configurations",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_provider_currency",
                        columnNames = {"provider_id", "currency_code"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_provider_currency_enabled",
                        columnList = "provider_id, currency_code, enabled"
                )
        }
)
public class ProviderCurrencyConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "provider_id", nullable = false)
    private PaymentProvider provider;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public ProviderCurrencyConfiguration() {
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;

        if (currencyCode != null) {
            currencyCode = currencyCode.trim()
                    .toUpperCase(java.util.Locale.ROOT);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();

        if (currencyCode != null) {
            currencyCode = currencyCode.trim()
                    .toUpperCase(java.util.Locale.ROOT);
        }
    }

    public Long getId() {
        return id;
    }

    public PaymentProvider getProvider() {
        return provider;
    }

    public void setProvider(PaymentProvider provider) {
        this.provider = provider;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
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
