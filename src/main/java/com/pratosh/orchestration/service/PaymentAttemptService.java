package com.pratosh.orchestration.service;

import com.pratosh.orchestration.entity.PaymentAttempt;
import com.pratosh.orchestration.entity.PaymentAttemptStatus;
import com.pratosh.orchestration.entity.PaymentFailureCategory;
import com.pratosh.orchestration.entity.PaymentProvider;
import com.pratosh.orchestration.entity.PaymentTransaction;
import com.pratosh.orchestration.entity.PaymentTransactionStatus;
import com.pratosh.orchestration.repository.PaymentAttemptRepository;
import com.pratosh.orchestration.repository.PaymentProviderRepository;
import com.pratosh.orchestration.repository.PaymentTransactionRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PaymentAttemptService {

    private final PaymentAttemptRepository attemptRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final PaymentProviderRepository providerRepository;
    private final PaymentRetryPolicy retryPolicy;
    private final PaymentRoutingService routingService;

    @Value("${payment.routing.environment:SANDBOX}")
    private String routingEnvironment;

    public PaymentAttemptService(
            PaymentAttemptRepository attemptRepository,
            PaymentTransactionRepository transactionRepository,
            PaymentProviderRepository providerRepository,
            PaymentRetryPolicy retryPolicy,
            PaymentRoutingService routingService) {

        this.attemptRepository = attemptRepository;
        this.transactionRepository = transactionRepository;
        this.providerRepository = providerRepository;
        this.retryPolicy = retryPolicy;
        this.routingService = routingService;
    }

    /**
     * Creates the initial attempt or a retry attempt.
     */
    @Transactional
    public PaymentAttempt createAttempt(
            Long transactionId,
            Long providerId) {

        if (transactionId == null) {
            throw new IllegalArgumentException(
                    "Transaction ID is required.");
        }

        if (providerId == null) {
            throw new IllegalArgumentException(
                    "Provider ID is required.");
        }

        PaymentTransaction transaction =
                transactionRepository.findById(transactionId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Payment transaction not found: "
                                                + transactionId));

        if (transaction.getStatus()
                != PaymentTransactionStatus.CREATED) {
            throw new IllegalStateException(
                    "A new attempt can only be created when the "
                            + "transaction status is CREATED.");
        }

        PaymentAttempt latestAttempt =
                attemptRepository
                        .findFirstByTransaction_IdOrderByAttemptNumberDesc(
                                transactionId)
                        .orElse(null);

        PaymentProvider selectedProvider;

        if (latestAttempt == null) {

            // Validate the provider for the initial attempt.
            selectedProvider =
                    providerRepository.findById(providerId)
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "Payment provider not found: "
                                                    + providerId));

            if (!selectedProvider.isEnabled()) {
                throw new IllegalStateException(
                        "The selected payment provider is disabled.");
            }

            if (transaction.getProvider() == null
                    || !transaction.getProvider().getId()
                    .equals(selectedProvider.getId())) {
                throw new IllegalArgumentException(
                        "The provider does not match the provider "
                                + "selected for this transaction.");
            }

        } else {

            // Only an ordinary FAILED attempt can be retried.
            if (latestAttempt.getStatus()
                    != PaymentAttemptStatus.FAILED) {
                throw new IllegalStateException(
                        "A retry can only be created after the "
                                + "previous attempt has failed.");
            }

            if (!retryPolicy.isRetryEnabled()) {
                throw new IllegalStateException(
                        "Payment retries are disabled.");
            }

            int attemptsMade = latestAttempt.getAttemptNumber();

            if (!retryPolicy.canRetry(
                    latestAttempt.getFailureCategory(),
                    attemptsMade)) {
                throw new IllegalStateException(
                        "The previous payment failure is not "
                                + "eligible for retry.");
            }

            // Select an eligible alternative provider.
            selectedProvider =
                    routingService.selectAlternativeProvider(
                            transaction.getPaymentMethod(),
                            transaction.getCurrency(),
                            routingEnvironment,
                            latestAttempt.getProvider().getId());
        }

        int nextAttemptNumber = latestAttempt == null
                ? 1
                : latestAttempt.getAttemptNumber() + 1;

        // Guard against duplicate attempt numbers.
        if (attemptRepository
                .existsByTransaction_IdAndAttemptNumber(
                        transactionId,
                        nextAttemptNumber)) {
            throw new IllegalStateException(
                    "Attempt number " + nextAttemptNumber
                            + " already exists for transaction "
                            + transactionId + ".");
        }

        PaymentAttempt attempt = new PaymentAttempt();
        attempt.setTransaction(transaction);
        attempt.setProvider(selectedProvider);
        attempt.setAttemptNumber(nextAttemptNumber);
        attempt.setStatus(PaymentAttemptStatus.CREATED);

        // Save the attempt and synchronize the transaction counter.
        PaymentAttempt savedAttempt =
                attemptRepository.save(attempt);

        transaction.setAttemptCount(nextAttemptNumber);
        transactionRepository.save(transaction);

        return savedAttempt;
    }

    /**
     * Moves an attempt from CREATED to PROCESSING.
     */
    @Transactional
    public PaymentAttempt startAttempt(Long attemptId) {

        PaymentAttempt attempt = getAttemptForUpdate(attemptId);

        if (attempt.getStatus() != PaymentAttemptStatus.CREATED) {
            throw new IllegalStateException(
                    "Only a CREATED attempt can be started.");
        }

        PaymentTransaction transaction = attempt.getTransaction();

        if (transaction.getStatus()
                != PaymentTransactionStatus.CREATED) {
            throw new IllegalStateException(
                    "Only a transaction with status CREATED "
                            + "can start a payment attempt.");
        }

        if (!attempt.getProvider().isEnabled()) {
            throw new IllegalStateException(
                    "Cannot start an attempt because its "
                            + "payment provider is disabled.");
        }

        attempt.setStatus(PaymentAttemptStatus.PROCESSING);
        attempt.setStartedAt(LocalDateTime.now());

        transaction.setStatus(PaymentTransactionStatus.PROCESSING);

        attemptRepository.save(attempt);
        transactionRepository.save(transaction);

        return attempt;
    }

    /**
     * Marks a processing attempt as successful.
     */
    @Transactional
    public PaymentAttempt markAttemptSuccessful(
            Long attemptId,
            String providerTransactionReference) {

        PaymentAttempt attempt = getAttemptForUpdate(attemptId);

        if (attempt.getStatus() != PaymentAttemptStatus.PROCESSING) {
            throw new IllegalStateException(
                    "Only a PROCESSING attempt can be marked successful.");
        }

        if (providerTransactionReference == null
                || providerTransactionReference.isBlank()) {
            throw new IllegalArgumentException(
                    "Provider transaction reference is required.");
        }

        PaymentTransaction transaction = attempt.getTransaction();

        if (transaction.getStatus()
                == PaymentTransactionStatus.SUCCESS) {
            throw new IllegalStateException(
                    "This transaction has already succeeded.");
        }

        attempt.setStatus(PaymentAttemptStatus.SUCCESS);
        attempt.setProviderTransactionReference(
                providerTransactionReference);
        attempt.setCompletedAt(LocalDateTime.now());

        transaction.setStatus(PaymentTransactionStatus.SUCCESS);
        transaction.setProviderTransactionReference(
                providerTransactionReference);
        transaction.setProvider(attempt.getProvider());

        attemptRepository.save(attempt);
        transactionRepository.save(transaction);

        return attempt;
    }

    /**
     * Marks a processing attempt as failed and updates the transaction.
     */
    @Transactional
    public PaymentAttempt markAttemptFailed(
            Long attemptId,
            String failureReason,
            PaymentFailureCategory failureCategory) {

        PaymentAttempt attempt = getAttemptForUpdate(attemptId);

        if (attempt.getStatus() != PaymentAttemptStatus.PROCESSING) {
            throw new IllegalStateException(
                    "Only a PROCESSING attempt can be marked failed.");
        }

        if (failureReason == null || failureReason.isBlank()) {
            throw new IllegalArgumentException(
                    "Failure reason is required.");
        }

        if (failureCategory == null) {
            throw new IllegalArgumentException(
                    "Failure category is required.");
        }

        PaymentTransaction transaction = attempt.getTransaction();

        attempt.setFailureReason(failureReason);
        attempt.setFailureCategory(failureCategory);
        attempt.setCompletedAt(LocalDateTime.now());

        // TIMEOUT and UNKNOWN are not ordinary retryable failures.
        switch (failureCategory) {
            case TIMEOUT ->
                    attempt.setStatus(PaymentAttemptStatus.TIMEOUT);

            case UNKNOWN ->
                    attempt.setStatus(PaymentAttemptStatus.UNKNOWN);

            default ->
                    attempt.setStatus(PaymentAttemptStatus.FAILED);
        }

        if (failureCategory == PaymentFailureCategory.TIMEOUT
                || failureCategory == PaymentFailureCategory.UNKNOWN) {

            transaction.setStatus(PaymentTransactionStatus.UNKNOWN);

        } else if (retryPolicy.canRetry(
                failureCategory,
                attempt.getAttemptNumber())) {

            transaction.setStatus(PaymentTransactionStatus.CREATED);

        } else {

            transaction.setStatus(PaymentTransactionStatus.FAILED);
        }

        attemptRepository.save(attempt);
        transactionRepository.save(transaction);

        return attempt;
    }

    /**
     * Checks whether the latest attempt can be retried.
     */
    @Transactional(Transactional.TxType.SUPPORTS)
    public boolean canRetryTransaction(Long transactionId) {

        if (transactionId == null) {
            return false;
        }

        PaymentTransaction transaction =
                transactionRepository.findById(transactionId)
                        .orElse(null);

        if (transaction == null
                || transaction.getStatus()
                != PaymentTransactionStatus.CREATED) {
            return false;
        }

        PaymentAttempt latestAttempt =
                attemptRepository
                        .findFirstByTransaction_IdOrderByAttemptNumberDesc(
                                transactionId)
                        .orElse(null);

        if (latestAttempt == null
                || latestAttempt.getStatus()
                != PaymentAttemptStatus.FAILED) {
            return false;
        }

        return retryPolicy.canRetry(
                latestAttempt.getFailureCategory(),
                latestAttempt.getAttemptNumber());
    }

    /**
     * Retrieves all attempts, newest first.
     */
    @Transactional(Transactional.TxType.SUPPORTS)
    public List<PaymentAttempt> getAllAttempts() {
        return attemptRepository.findAll(
                org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.DESC,
                        "id"));
    }

    /**
     * Retrieves an attempt by ID.
     */
    @Transactional(Transactional.TxType.SUPPORTS)
    public PaymentAttempt getAttempt(Long attemptId) {

        if (attemptId == null) {
            throw new IllegalArgumentException(
                    "Attempt ID is required.");
        }

        return attemptRepository.findById(attemptId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Payment attempt not found: "
                                        + attemptId));
    }

    /**
     * Retrieves all attempts for a transaction in attempt order.
     */
    @Transactional(Transactional.TxType.SUPPORTS)
    public List<PaymentAttempt> getAttemptsForTransaction(
            Long transactionId) {

        if (transactionId == null) {
            throw new IllegalArgumentException(
                    "Transaction ID is required.");
        }

        return attemptRepository
                .findByTransaction_IdOrderByAttemptNumberAsc(
                        transactionId);
    }

    /**
     * Retrieves an attempt within the current transaction.
     */
    private PaymentAttempt getAttemptForUpdate(Long attemptId) {

        if (attemptId == null) {
            throw new IllegalArgumentException(
                    "Attempt ID is required.");
        }

        return attemptRepository.findById(attemptId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Payment attempt not found: "
                                        + attemptId));
    }
}
