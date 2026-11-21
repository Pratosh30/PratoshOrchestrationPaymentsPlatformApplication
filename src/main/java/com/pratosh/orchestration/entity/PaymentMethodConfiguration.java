package com.pratosh.orchestration.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;


 //Stores the payment methods configured for each payment provider.
 //Example:   Razorpay + UPI + enabled | Razorpay + CREDIT_CARD + enabled | PayU + BANK_TRANSFER + disabled

@Entity
@Table(
        name = "payment_method_configurations",
        uniqueConstraints = {
                // A provider must not have duplicate configurations
                // for the same payment method.
                @UniqueConstraint(
                        name = "uk_provider_payment_method",
                        columnNames = {"provider_id", "payment_method"}
                )
        }
)
public class PaymentMethodConfiguration {

    // Primary key generated automatically by the database.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The payment provider associated with this configuration.
    // Many payment method configurations can belong to one provider.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "provider_id", nullable = false)
    private PaymentProvider provider;

    // The payment method being configured.
    // EnumType.STRING stores values such as UPI and CREDIT_CARD.
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    private PaymentMethod paymentMethod;

    // Indicates whether this method is enabled in our platform.
    @Column(nullable = false)
    private boolean enabled = true;

    // Records when the configuration was created.
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Records the most recent modification time.
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Sets timestamps before the entity is first inserted.
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    // Updates the modification timestamp before an update.
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Required by JPA.
    public PaymentMethodConfiguration() {
    }

    // Getters and setters allow other application layers
    // to access and update the entity fields.

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

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}