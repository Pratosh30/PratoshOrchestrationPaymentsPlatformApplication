package com.pratosh.orchestration.gateway;

import com.pratosh.orchestration.entity.PaymentFailureCategory;

/**
 * Outcome returned by a payment gateway adapter.
 */
public record GatewayResult(
        boolean success,
        String providerReference,
        String failureReason,
        PaymentFailureCategory failureCategory) {

    public static GatewayResult success(String providerReference) {
        return new GatewayResult(true, providerReference, null, null);
    }

    public static GatewayResult failure(
            String reason, PaymentFailureCategory category) {
        return new GatewayResult(false, null, reason, category);
    }
}
