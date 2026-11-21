package com.pratosh.orchestration.service;

import com.pratosh.orchestration.entity.PaymentFailureCategory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class PaymentRetryPolicy {

    private static final Set<PaymentFailureCategory> RETRYABLE_FAILURES =
            Set.of(
                    PaymentFailureCategory.NETWORK_ERROR,
                    PaymentFailureCategory.PROVIDER_UNAVAILABLE,
                    PaymentFailureCategory.TEMPORARY_PROVIDER_ERROR
            );

    @Value("${payment.retry.enabled:true}")
    private boolean retryEnabled;

    @Value("${payment.retry.max-attempts:3}")
    private int maxAttempts;

    @Value("${payment.retry.delay-ms:2000}")
    private long retryDelayMs;

    /**
     * Determines whether another attempt is permitted.
     *
     * attemptsMade includes the initial attempt.
     */
    public boolean canRetry(
            PaymentFailureCategory failureCategory,
            int attemptsMade) {

        if (!retryEnabled) {
            return false;
        }

        if (maxAttempts < 1 || attemptsMade < 1) {
            return false;
        }

        if (attemptsMade >= maxAttempts) {
            return false;
        }

        if (failureCategory == null) {
            return false;
        }

        // TIMEOUT and UNKNOWN are deliberately excluded.
        // Verify the payment outcome before considering another attempt.
        return RETRYABLE_FAILURES.contains(failureCategory);
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public long getRetryDelayMs() {
        return retryDelayMs;
    }

    public boolean isRetryEnabled() {
        return retryEnabled;
    }
}
