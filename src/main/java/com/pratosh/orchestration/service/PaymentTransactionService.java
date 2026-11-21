package com.pratosh.orchestration.service;

import com.pratosh.orchestration.dto.CreatePaymentRequest;
import com.pratosh.orchestration.dto.PaymentTransactionResponse;
import com.pratosh.orchestration.entity.PaymentMethod;
import com.pratosh.orchestration.entity.PaymentProvider;
import com.pratosh.orchestration.entity.PaymentTransaction;
import com.pratosh.orchestration.entity.PaymentTransactionStatus;
import com.pratosh.orchestration.repository.PaymentTransactionRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class PaymentTransactionService {

    private final PaymentTransactionRepository transactionRepository;
    private final PaymentRoutingService paymentRoutingService;

    @Value("${payment.routing.environment:SANDBOX}")
    private String routingEnvironment;

    public PaymentTransactionService(
            PaymentTransactionRepository transactionRepository,
            PaymentRoutingService paymentRoutingService) {

        this.transactionRepository = transactionRepository;
        this.paymentRoutingService = paymentRoutingService;
    }

    /**
     * Creates a payment transaction after validating the request
     * and selecting an eligible payment provider.
     *
     * This method creates a transaction record. It does not yet
     * initiate a real payment with the selected provider.
     */
    @Transactional
    public PaymentTransactionResponse createTransaction(
            CreatePaymentRequest request) {
        return createTransaction(request, null);
    }

    @Transactional
    public PaymentTransactionResponse createTransaction(
            CreatePaymentRequest request,
            String idempotencyKey) {

        // 1. Validate the request body.
        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Payment request cannot be empty."
            );
        }

        // 2. Validate the merchant order ID.
        if (request.getMerchantOrderId() == null
                || request.getMerchantOrderId().isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Merchant order ID is required."
            );
        }

        String merchantOrderId =
                request.getMerchantOrderId().trim();

        // 2b. Idempotent replay: the same key returns the original
        // transaction; reusing a key for a different request is rejected.
        String key = idempotencyKey == null ? null : idempotencyKey.trim();

        if (key != null && key.isEmpty()) {
            key = null;
        }

        if (key != null && key.length() > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Idempotency-Key cannot exceed 100 characters."
            );
        }

        if (key != null) {
            java.util.Optional<PaymentTransaction> existing =
                    transactionRepository.findByIdempotencyKey(key);

            if (existing.isPresent()) {
                PaymentTransaction original = existing.get();

                boolean sameRequest =
                        original.getMerchantOrderId().equals(merchantOrderId)
                                && request.getAmount() != null
                                && original.getAmount()
                                .compareTo(request.getAmount()) == 0
                                && request.getCurrency() != null
                                && original.getCurrency().equalsIgnoreCase(
                                request.getCurrency().trim())
                                && original.getPaymentMethod()
                                == request.getPaymentMethod();

                if (!sameRequest) {
                    throw new ResponseStatusException(
                            HttpStatus.UNPROCESSABLE_ENTITY,
                            "Idempotency-Key was already used with a different request."
                    );
                }

                return PaymentTransactionResponse.fromEntity(original);
            }
        }

        // 3. Check whether this order has already been submitted.
        if (transactionRepository.existsByMerchantOrderId(
                merchantOrderId)) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A transaction with this merchant order ID already exists."
            );
        }

        // 4. Validate the payment amount.
        BigDecimal amount = request.getAmount();

        if (amount == null
                || amount.compareTo(BigDecimal.ZERO) <= 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Payment amount must be greater than zero."
            );
        }

        // 5. Validate and normalize the currency.
        if (request.getCurrency() == null
                || !request.getCurrency()
                .trim()
                .matches("(?i)[A-Z]{3}")) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Currency must be a three-letter code, such as INR."
            );
        }

        String currency = request.getCurrency()
                .trim()
                .toUpperCase(Locale.ROOT);

        // 6. Validate the payment method.
        PaymentMethod paymentMethod = request.getPaymentMethod();

        if (paymentMethod == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Payment method is required."
            );
        }

        // 7. Validate the configured routing environment.
        if (routingEnvironment == null
                || routingEnvironment.isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Payment routing environment is not configured."
            );
        }

        String environment = routingEnvironment
                .trim()
                .toUpperCase(Locale.ROOT);

        if (!environment.equals("SANDBOX")
                && !environment.equals("PRODUCTION")) {

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Payment routing environment must be SANDBOX or PRODUCTION."
            );
        }

        // 8. Select an eligible provider using routing priority,
        // payment method, currency, environment, and enabled credentials.
        PaymentProvider selectedProvider;

        try {
            selectedProvider = paymentRoutingService.selectProvider(
                    paymentMethod,
                    currency,
                    environment
            );
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    ex.getMessage(),
                    ex
            );
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    ex.getMessage(),
                    ex
            );
        }

        // 9. Create the transaction record.
        PaymentTransaction transaction = new PaymentTransaction();

        transaction.setTransactionReference(
                "PAY-" + UUID.randomUUID()
        );

        transaction.setMerchantOrderId(merchantOrderId);
        transaction.setAmount(amount);
        transaction.setCurrency(currency);
        transaction.setPaymentMethod(paymentMethod);
        transaction.setStatus(PaymentTransactionStatus.CREATED);
        transaction.setProvider(selectedProvider);
        transaction.setAttemptCount(0);
        transaction.setIdempotencyKey(key);

        // 10. Save immediately so database constraint violations
        // can be handled here.
        try {
            PaymentTransaction savedTransaction =
                    transactionRepository.saveAndFlush(transaction);

            return PaymentTransactionResponse.fromEntity(
                    savedTransaction
            );

        } catch (DataIntegrityViolationException ex) {
            // The database unique constraint provides protection
            // against simultaneous requests using the same order ID.
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "The transaction could not be created because a database constraint was violated.",
                    ex
            );
        }
    }

    /**
     * Filtered, paginated search (newest first). All filters are optional;
     * the "to" date is inclusive.
     */
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<PaymentTransactionResponse>
    search(PaymentTransactionStatus status,
           Long providerId,
           PaymentMethod paymentMethod,
           java.time.LocalDate from,
           java.time.LocalDate to,
           int page,
           int size) {

        java.time.LocalDateTime start =
                from == null ? null : from.atStartOfDay();
        java.time.LocalDateTime end =
                to == null ? null : to.plusDays(1).atStartOfDay();

        org.springframework.data.domain.Pageable pageable =
                org.springframework.data.domain.PageRequest.of(
                        Math.max(page, 0),
                        Math.min(Math.max(size, 1), 500),
                        org.springframework.data.domain.Sort.by(
                                org.springframework.data.domain.Sort
                                        .Direction.DESC,
                                "id"));

        return transactionRepository
                .search(status, providerId, paymentMethod, start, end,
                        pageable)
                .map(PaymentTransactionResponse::fromEntity);
    }

    /**
     * Retrieves all transactions, newest first.
     */
    @Transactional(readOnly = true)
    public java.util.List<PaymentTransactionResponse> getAllTransactions() {
        return transactionRepository.findAll(
                        org.springframework.data.domain.Sort.by(
                                org.springframework.data.domain.Sort.Direction.DESC,
                                "id"))
                .stream()
                .map(PaymentTransactionResponse::fromEntity)
                .toList();
    }

    /**
     * Retrieves a transaction using its transaction reference.
     */
    @Transactional(readOnly = true)
    public PaymentTransactionResponse getTransactionByReference(
            String transactionReference) {

        if (transactionReference == null
                || transactionReference.isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Transaction reference is required."
            );
        }

        PaymentTransaction transaction =
                transactionRepository.findByTransactionReference(
                        transactionReference.trim()
                ).orElseThrow(() ->
                        new NoSuchElementException(
                                "Transaction not found: "
                                        + transactionReference
                        )
                );

        return PaymentTransactionResponse.fromEntity(transaction);
    }
}
