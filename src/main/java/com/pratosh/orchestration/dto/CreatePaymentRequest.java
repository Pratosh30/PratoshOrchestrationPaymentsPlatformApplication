package com.pratosh.orchestration.dto;

import com.pratosh.orchestration.entity.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class CreatePaymentRequest {

    @NotBlank(message = "Merchant order ID is required")
    @Size(max = 100, message = "Merchant order ID cannot exceed 100 characters")
    private String merchantOrderId;

    @NotNull(message = "Payment amount is required")
    @DecimalMin(value = "0.01", message = "Payment amount must be greater than zero")
    @Digits(
            integer = 15,
            fraction = 4,
            message = "Amount must contain at most 15 integer digits and 4 decimal places"
    )
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    @Pattern(
            regexp = "(?i)[A-Z]{3}",
            message = "Currency must be a three-letter code, such as INR or USD"
    )
    private String currency;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    public CreatePaymentRequest() {
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
}
