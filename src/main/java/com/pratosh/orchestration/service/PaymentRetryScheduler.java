package com.pratosh.orchestration.service;

import com.pratosh.orchestration.entity.PaymentAttempt;
import com.pratosh.orchestration.entity.PaymentAttemptStatus;
import com.pratosh.orchestration.entity.PaymentFailureCategory;
import com.pratosh.orchestration.entity.PaymentTransaction;
import com.pratosh.orchestration.entity.PaymentTransactionStatus;
import com.pratosh.orchestration.repository.PaymentAttemptRepository;
import com.pratosh.orchestration.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentRetryScheduler {

    private final PaymentAttemptRepository paymentAttemptRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentRetryPolicy paymentRetryPolicy;
    private final PaymentAttemptService paymentAttemptService;
    private final PaymentRoutingService paymentRoutingService;

    @Value("${payment.retry.enabled:true}")
    private boolean retryEnabled;

    @Value("${payment.retry.delay-ms:2000}")
    private long retryDelayMs;

    @Value("${payment.routing.environment:SANDBOX}")
    private String routingEnvironment;

    /**
     * Periodically checks failed payment attempts and prepares eligible retries.
     */
    @Scheduled(fixedDelayString = "${payment.retry.scan-interval-ms:5000}")
    @Transactional
    public void scheduleEligibleRetries() {

        if (!retryEnabled || !paymentRetryPolicy.isRetryEnabled()) {
            return;
        }

        List<PaymentAttempt> failedAttempts =
                paymentAttemptRepository.findByStatusAndTransaction_Status(
                        PaymentAttemptStatus.FAILED,
                        PaymentTransactionStatus.CREATED);

        for (PaymentAttempt failedAttempt : failedAttempts) {
            try {
                processFailedAttempt(failedAttempt);
            } catch (Exception exception) {
                log.error(
                        "Unable to prepare retry for attempt ID {}: {}",
                        failedAttempt.getId(),
                        exception.getMessage(),
                        exception
                );
            }
        }
    }

    /**
     * Evaluates whether a failed attempt is eligible for retry.
     */
    private void processFailedAttempt(PaymentAttempt failedAttempt) {

        if (failedAttempt.getCompletedAt() == null) {
            return;
        }

        if (failedAttempt.getFailureCategory() == null) {
            return;
        }

        PaymentTransaction transaction =
                paymentTransactionRepository.findById(
                        failedAttempt.getTransaction().getId()
                ).orElse(null);

        if (transaction == null) {
            log.warn(
                    "Transaction not found for failed attempt ID {}",
                    failedAttempt.getId()
            );
            return;
        }

        if (transaction.getStatus() != PaymentTransactionStatus.CREATED) {
            return;
        }

        PaymentAttempt latestAttempt =
                paymentAttemptRepository
                        .findFirstByTransaction_IdOrderByAttemptNumberDesc(
                                transaction.getId()
                        )
                        .orElse(null);

        if (latestAttempt == null) {
            return;
        }

        // Do not retry an older failed attempt if a newer attempt exists.
        if (!latestAttempt.getId().equals(failedAttempt.getId())) {
            return;
        }

        if (latestAttempt.getStatus() != PaymentAttemptStatus.FAILED) {
            return;
        }

        PaymentFailureCategory failureCategory =
                latestAttempt.getFailureCategory();

        if (failureCategory == null) {
            return;
        }

        if (!paymentRetryPolicy.canRetry(
                failureCategory,
                latestAttempt.getAttemptNumber()
        )) {
            log.info(
                    "Retry limit reached or failure is not retryable for transaction ID {}",
                    transaction.getId()
            );
            return;
        }

        LocalDateTime completedAt = latestAttempt.getCompletedAt();

        if (completedAt.plusNanos(retryDelayMs * 1_000_000L)
                .isAfter(LocalDateTime.now())) {
            return;
        }

        Long previousProviderId = latestAttempt.getProvider().getId();

        com.pratosh.orchestration.entity.PaymentProvider alternativeProvider;

        try {
            alternativeProvider =
                    paymentRoutingService.selectAlternativeProvider(
                            transaction.getPaymentMethod(),
                            transaction.getCurrency(),
                            routingEnvironment,
                            previousProviderId
                    );
        } catch (IllegalStateException noProvider) {
            // Retries are exhausted in practice: no other provider can take
            // this payment. Move the transaction to a final state instead of
            // leaving it in CREATED and re-evaluating it on every scan.
            transaction.setStatus(PaymentTransactionStatus.FAILED);
            paymentTransactionRepository.save(transaction);

            log.warn(
                    "No eligible alternative provider for transaction ID {}; marked FAILED",
                    transaction.getId()
            );
            return;
        }

        paymentAttemptService.createAttempt(
                transaction.getId(),
                alternativeProvider.getId()
        );

        log.info(
                "Prepared retry attempt for transaction ID {} using provider ID {}",
                transaction.getId(),
                alternativeProvider.getId()
        );
    }
}
