package com.pratosh.orchestration.dto;

import com.pratosh.orchestration.entity.PaymentMethod;
import jakarta.validation.constraints.NotNull;

/*
  DTO for incoming payment method configuration requests.
 Example request:
  {
   "paymentMethod": "UPI"
  }
 */


public class PaymentMethodConfigurationRequest {

    /*
     The payment method the administrator wants to configure.
       Accepted values:
       CREDIT_CARD, DEBIT_CARD, UPI, BANK_TRANSFER, QR_CODE
     */
    @NotNull(message = "Payment method is required.")
    private PaymentMethod paymentMethod;

    // Default constructor required for JSON deserialization.
    public PaymentMethodConfigurationRequest() {
    }

    // Getter used by the service/controller to read the value.
    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    // Setter used by Jackson when converting JSON into this DTO.
    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
}