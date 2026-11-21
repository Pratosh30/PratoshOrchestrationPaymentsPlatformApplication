package com.pratosh.orchestration.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "payment_attempts",
        indexes = {
                @Index(
                        name = "idx_attempt_transaction_id",
                        columnList = "transaction_id"
                ),
                @Index(
                        name = "idx_attempt_status",
                        columnList = "status"
                )
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_transaction_attempt_number",
                        columnNames = {"transaction_id", "attempt_number"}
                )
        }
)
public class PaymentAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The original payment transaction associated with this attempt.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false)
    private PaymentTransaction transaction;

    // The payment provider selected for this attempt.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "provider_id", nullable = false)
    private PaymentProvider provider;

    // Sequence number: 1, 2, 3, etc.
    @Column(name = "attempt_number", nullable = false)
    private Integer attemptNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentAttemptStatus status = PaymentAttemptStatus.CREATED;

    // Structured category used by the retry policy.
    @Enumerated(EnumType.STRING)
    @Column(name = "failure_category", length = 40)
    private PaymentFailureCategory failureCategory;

    // Reference returned by the payment provider, when available.
    @Column(name = "provider_transaction_reference", length = 150)
    private String providerTransactionReference;

    // Safe diagnostic information; never store credentials or card data.
    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }

        if (status == null) {
            status = PaymentAttemptStatus.CREATED;
        }
    }

    public Long getId() {
        return id;
    }

    public PaymentTransaction getTransaction() {
        return transaction;
    }

    public void setTransaction(PaymentTransaction transaction) {
        this.transaction = transaction;
    }

    public PaymentProvider getProvider() {
        return provider;
    }

    public void setProvider(PaymentProvider provider) {
        this.provider = provider;
    }

    public Integer getAttemptNumber() {
        return attemptNumber;
    }

    public void setAttemptNumber(Integer attemptNumber) {
        this.attemptNumber = attemptNumber;
    }

    public PaymentAttemptStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentAttemptStatus status) {
        this.status = status;
    }

    public PaymentFailureCategory getFailureCategory() {
        return failureCategory;
    }

    public void setFailureCategory(PaymentFailureCategory failureCategory) {
        this.failureCategory = failureCategory;
    }

    public String getProviderTransactionReference() {
        return providerTransactionReference;
    }

    public void setProviderTransactionReference(
            String providerTransactionReference) {
        this.providerTransactionReference = providerTransactionReference;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
