package com.pratosh.orchestration.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One line of a reconciliation run: the platform's view compared with the
 * provider's report. Records are never deleted; reviewing a discrepancy
 * only adds a note, which keeps an audit trail.
 */
@Entity
@Table(
        name = "reconciliation_records",
        indexes = {
                @Index(name = "idx_recon_batch", columnList = "batch_id"),
                @Index(name = "idx_recon_result", columnList = "result")
        }
)
public class ReconciliationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_id", nullable = false, length = 36)
    private String batchId;

    @Column(name = "provider_id", nullable = false)
    private Long providerId;

    @Column(name = "transaction_reference", length = 100)
    private String transactionReference;

    @Column(name = "provider_transaction_reference", length = 150)
    private String providerTransactionReference;

    @Column(name = "platform_status", length = 30)
    private String platformStatus;

    @Column(name = "provider_status", length = 30)
    private String providerStatus;

    @Column(name = "platform_amount", precision = 19, scale = 4)
    private BigDecimal platformAmount;

    @Column(name = "provider_amount", precision = 19, scale = 4)
    private BigDecimal providerAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReconciliationResult result;

    @Column(length = 500)
    private String notes;

    @Column(nullable = false)
    private boolean reviewed = false;

    @Column(name = "review_note", length = 500)
    private String reviewNote;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }
    public Long getProviderId() { return providerId; }
    public void setProviderId(Long providerId) { this.providerId = providerId; }
    public String getTransactionReference() { return transactionReference; }
    public void setTransactionReference(String v) { this.transactionReference = v; }
    public String getProviderTransactionReference() { return providerTransactionReference; }
    public void setProviderTransactionReference(String v) { this.providerTransactionReference = v; }
    public String getPlatformStatus() { return platformStatus; }
    public void setPlatformStatus(String v) { this.platformStatus = v; }
    public String getProviderStatus() { return providerStatus; }
    public void setProviderStatus(String v) { this.providerStatus = v; }
    public BigDecimal getPlatformAmount() { return platformAmount; }
    public void setPlatformAmount(BigDecimal v) { this.platformAmount = v; }
    public BigDecimal getProviderAmount() { return providerAmount; }
    public void setProviderAmount(BigDecimal v) { this.providerAmount = v; }
    public ReconciliationResult getResult() { return result; }
    public void setResult(ReconciliationResult result) { this.result = result; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public boolean isReviewed() { return reviewed; }
    public void setReviewed(boolean reviewed) { this.reviewed = reviewed; }
    public String getReviewNote() { return reviewNote; }
    public void setReviewNote(String reviewNote) { this.reviewNote = reviewNote; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
