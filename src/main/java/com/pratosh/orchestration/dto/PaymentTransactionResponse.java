package com.pratosh.orchestration.dto;

import com.pratosh.orchestration.entity.PaymentTransaction;
import com.pratosh.orchestration.entity.PaymentTransactionStatus;
import com.pratosh.orchestration.entity.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentTransactionResponse {

    private Long id;
    private String transactionReference;
    private String merchantOrderId;
    private BigDecimal amount;
    private String currency;
    private PaymentMethod paymentMethod;
    private PaymentTransactionStatus status;
    private Long providerId;
    private String providerName;
    private String providerTransactionReference;
    private int attemptCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public PaymentTransactionResponse() {
    }

    public static PaymentTransactionResponse fromEntity(
            PaymentTransaction transaction) {

        PaymentTransactionResponse response =
                new PaymentTransactionResponse();

        response.setId(transaction.getId());
        response.setTransactionReference(
                transaction.getTransactionReference());
        response.setMerchantOrderId(transaction.getMerchantOrderId());
        response.setAmount(transaction.getAmount());
        response.setCurrency(transaction.getCurrency());
        response.setPaymentMethod(transaction.getPaymentMethod());
        response.setStatus(transaction.getStatus());
        response.setProviderTransactionReference(
                transaction.getProviderTransactionReference());
        response.setAttemptCount(transaction.getAttemptCount());
        response.setCreatedAt(transaction.getCreatedAt());
        response.setUpdatedAt(transaction.getUpdatedAt());

        if (transaction.getProvider() != null) {
            response.setProviderId(transaction.getProvider().getId());
            response.setProviderName(
                    transaction.getProvider().getProviderName());
        }

        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }

    public String getMerchantOrderId() {
        return merchantOrderId;
    }

    public void setMerchantOrderId(String merchantOrderId) {
        this.merchantOrderId = merchantOrderId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public PaymentTransactionStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentTransactionStatus status) {
        this.status = status;
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

    public String getProviderTransactionReference() {
        return providerTransactionReference;
    }

    public void setProviderTransactionReference(
            String providerTransactionReference) {
        this.providerTransactionReference = providerTransactionReference;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(int attemptCount) {
        this.attemptCount = attemptCount;
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
