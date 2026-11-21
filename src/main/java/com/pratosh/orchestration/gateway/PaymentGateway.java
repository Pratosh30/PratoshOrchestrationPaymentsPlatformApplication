package com.pratosh.orchestration.gateway;

import com.pratosh.orchestration.entity.PaymentProvider;
import com.pratosh.orchestration.entity.PaymentTransaction;

/**
 * Adapter between the orchestrator and one payment provider API.
 *
 * Real provider integrations (Razorpay, PayU, ...) implement this interface.
 * They must load credentials through the credential service, verify provider
 * signatures, and report timeouts as PaymentFailureCategory.TIMEOUT so the
 * outcome is verified before any retry.
 */
public interface PaymentGateway {

    GatewayResult process(PaymentTransaction transaction,
                          PaymentProvider provider);
}
