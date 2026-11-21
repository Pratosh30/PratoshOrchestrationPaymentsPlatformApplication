package com.pratosh.orchestration.gateway;

import com.pratosh.orchestration.entity.PaymentFailureCategory;
import com.pratosh.orchestration.entity.PaymentProvider;
import com.pratosh.orchestration.entity.PaymentTransaction;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.UUID;

/**
 * SIMULATED gateway for development and demos. It moves no money.
 *
 * The outcome is chosen deterministically from the merchant order ID so
 * every scenario can be reproduced:
 *   contains FAIL_NETWORK   -> retryable NETWORK_ERROR
 *   contains FAIL_DECLINE   -> non-retryable PAYMENT_DECLINED
 *   contains FAIL_TIMEOUT   -> TIMEOUT (outcome uncertain, needs verification)
 *   anything else           -> success
 */
@Component
public class MockPaymentGateway implements PaymentGateway {

    @Override
    public GatewayResult process(PaymentTransaction transaction,
                                 PaymentProvider provider) {

        String order = transaction.getMerchantOrderId() == null
                ? ""
                : transaction.getMerchantOrderId().toUpperCase(Locale.ROOT);

        if (order.contains("FAIL_NETWORK")) {
            return GatewayResult.failure(
                    "Simulated network error",
                    PaymentFailureCategory.NETWORK_ERROR);
        }
        if (order.contains("FAIL_DECLINE")) {
            return GatewayResult.failure(
                    "Simulated payment declined",
                    PaymentFailureCategory.PAYMENT_DECLINED);
        }
        if (order.contains("FAIL_TIMEOUT")) {
            return GatewayResult.failure(
                    "Simulated provider timeout",
                    PaymentFailureCategory.TIMEOUT);
        }

        return GatewayResult.success(
                "MOCK-" + UUID.randomUUID());
    }
}
