
package com.pratosh.orchestration.entity;

public enum PaymentFailureCategory {

    // Temporary technical problems that may be retryable.
    NETWORK_ERROR,
    PROVIDER_UNAVAILABLE,
    TEMPORARY_PROVIDER_ERROR,

    // Payment outcomes that generally should not be retried automatically.
    PAYMENT_DECLINED,
    INSUFFICIENT_FUNDS,
    INVALID_PAYMENT_DETAILS,
    AUTHENTICATION_ERROR,

    // The payment outcome is uncertain and must be verified.
    TIMEOUT,
    UNKNOWN
}
