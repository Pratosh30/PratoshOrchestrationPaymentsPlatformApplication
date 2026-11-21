package com.pratosh.orchestration.entity;

//Defines the payment methods supported by the platform.
//Using an enum prevents arbitrary payment method names from being stored in the database.
public enum PaymentMethod {

    CREDIT_CARD, // Payments made using a credit card.

    DEBIT_CARD,     // Payments made using a debit card.

    UPI,     // Payments made through Unified Payments Interface.

    BANK_TRANSFER,     // Payments made directly through online banking.

    QR_CODE,      // Payments initiated by scanning a QR code.
    NET_BANKING   // Payments made through the customer's online banking portal.
}
