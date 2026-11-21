package com.pratosh.orchestration.service;

import com.pratosh.orchestration.entity.PaymentAttempt;
import com.pratosh.orchestration.entity.PaymentFailureCategory;
import com.pratosh.orchestration.gateway.GatewayResult;
import com.pratosh.orchestration.gateway.PaymentGateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Runs one payment attempt through the gateway adapter.
 *
 * Deliberately NOT transactional: the attempt is moved to PROCESSING and
 * committed first, the (possibly slow) provider call happens outside any
 * database transaction, and the outcome is committed afterwards. An
 * exception from the gateway is recorded as UNKNOWN, never as a plain
 * failure, because the provider may have processed the payment.
 */
@Service
public class PaymentProcessingService {

    private static final Logger log =
            LoggerFactory.getLogger(PaymentProcessingService.class);

    private final PaymentAttemptService attemptService;
    private final PaymentGateway gateway;

    public PaymentProcessingService(
            PaymentAttemptService attemptService,
            PaymentGateway gateway) {
        this.attemptService = attemptService;
        this.gateway = gateway;
    }

    public PaymentAttempt process(Long attemptId) {

        PaymentAttempt attempt = attemptService.startAttempt(attemptId);

        GatewayResult result;
        try {
            result = gateway.process(
                    attempt.getTransaction(), attempt.getProvider());
        } catch (RuntimeException exception) {
            log.error("Gateway error for attempt {}: {}",
                    attemptId, exception.getClass().getSimpleName());
            return attemptService.markAttemptFailed(
                    attemptId,
                    "Gateway error; outcome must be verified",
                    PaymentFailureCategory.UNKNOWN);
        }

        if (result.success()) {
            return attemptService.markAttemptSuccessful(
                    attemptId, result.providerReference());
        }

        return attemptService.markAttemptFailed(
                attemptId,
                result.failureReason(),
                result.failureCategory() == null
                        ? PaymentFailureCategory.UNKNOWN
                        : result.failureCategory());
    }
}
