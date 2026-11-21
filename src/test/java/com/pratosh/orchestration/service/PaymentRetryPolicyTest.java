package com.pratosh.orchestration.service;

import com.pratosh.orchestration.entity.PaymentFailureCategory;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class PaymentRetryPolicyTest {

    private PaymentRetryPolicy policy(boolean enabled, int maxAttempts) {
        PaymentRetryPolicy policy = new PaymentRetryPolicy();
        ReflectionTestUtils.setField(policy, "retryEnabled", enabled);
        ReflectionTestUtils.setField(policy, "maxAttempts", maxAttempts);
        return policy;
    }

    @Test
    void temporaryFailuresAreRetriedUntilTheLimit() {
        PaymentRetryPolicy policy = policy(true, 3);

        assertTrue(policy.canRetry(PaymentFailureCategory.NETWORK_ERROR, 1));
        assertTrue(policy.canRetry(PaymentFailureCategory.NETWORK_ERROR, 2));
        assertFalse(policy.canRetry(PaymentFailureCategory.NETWORK_ERROR, 3));
    }

    @Test
    void declinesAreNeverRetried() {
        assertFalse(policy(true, 3)
                .canRetry(PaymentFailureCategory.PAYMENT_DECLINED, 1));
    }

    @Test
    void uncertainOutcomesAreNeverRetriedBlindly() {
        PaymentRetryPolicy policy = policy(true, 3);

        assertFalse(policy.canRetry(PaymentFailureCategory.TIMEOUT, 1));
        assertFalse(policy.canRetry(PaymentFailureCategory.UNKNOWN, 1));
    }

    @Test
    void disabledPolicyNeverRetries() {
        assertFalse(policy(false, 3)
                .canRetry(PaymentFailureCategory.NETWORK_ERROR, 1));
    }
}
