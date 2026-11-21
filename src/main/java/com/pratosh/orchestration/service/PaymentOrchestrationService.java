package com.pratosh.orchestration.service;

import com.pratosh.orchestration.dto.CreatePaymentRequest;
import com.pratosh.orchestration.dto.PaymentTransactionResponse;
import com.pratosh.orchestration.entity.PaymentAttempt;
import com.pratosh.orchestration.entity.PaymentTransactionStatus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Creates a payment and, when enabled, immediately runs its first attempt.
 *
 * Deliberately NOT transactional: the transaction is committed first, so a
 * problem while processing can never roll the payment record back. If the
 * first attempt cannot be created or run, the transaction stays CREATED and
 * can be processed manually; the error is logged, not thrown.
 *
 * Replays of an Idempotency-Key return the existing transaction and never
 * start a second attempt (the transaction is no longer fresh).
 */
@Service
public class PaymentOrchestrationService {

    private static final Logger log =
            LoggerFactory.getLogger(PaymentOrchestrationService.class);

    private final PaymentTransactionService transactionService;
    private final PaymentAttemptService attemptService;
    private final PaymentProcessingService processingService;

    @Value("${payment.auto-process:true}")
    private boolean autoProcess;

    public PaymentOrchestrationService(
            PaymentTransactionService transactionService,
            PaymentAttemptService attemptService,
            PaymentProcessingService processingService) {
        this.transactionService = transactionService;
        this.attemptService = attemptService;
        this.processingService = processingService;
    }

    public PaymentTransactionResponse createAndProcess(
            CreatePaymentRequest request,
            String idempotencyKey) {

        PaymentTransactionResponse created =
                transactionService.createTransaction(
                        request, idempotencyKey);

        boolean fresh = created.getStatus() == PaymentTransactionStatus.CREATED
                && created.getAttemptCount() == 0
                && created.getProviderId() != null;

        if (!autoProcess || !fresh) {
            return created;
        }

        try {
            PaymentAttempt attempt = attemptService.createAttempt(
                    created.getId(), created.getProviderId());
            processingService.process(attempt.getId());
        } catch (RuntimeException exception) {
            log.warn("Automatic processing failed for transaction {}: {}",
                    created.getTransactionReference(),
                    exception.getMessage());
        }

        return transactionService.getTransactionByReference(
                created.getTransactionReference());
    }
}
