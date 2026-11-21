package com.pratosh.orchestration.controller;

import com.pratosh.orchestration.entity.PaymentAttempt;
import com.pratosh.orchestration.entity.PaymentFailureCategory;
import com.pratosh.orchestration.service.PaymentAttemptService;
import com.pratosh.orchestration.entity.PaymentFailureCategory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/payment-attempts")
public class PaymentAttemptController {

    private final PaymentAttemptService paymentAttemptService;
    private final com.pratosh.orchestration.service.PaymentProcessingService
            processingService;

    public PaymentAttemptController(
            PaymentAttemptService paymentAttemptService,
            com.pratosh.orchestration.service.PaymentProcessingService
                    processingService) {
        this.paymentAttemptService = paymentAttemptService;
        this.processingService = processingService;
    }

    // 0. List all attempts.
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllAttempts() {

        List<Map<String, Object>> response =
                paymentAttemptService.getAllAttempts()
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return ResponseEntity.ok(response);
    }

    // 1. Create a payment attempt.
    @PostMapping
    public ResponseEntity<Map<String, Object>> createAttempt(
            @RequestBody CreateAttemptRequest request) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Request body is required."
            );
        }

        PaymentAttempt attempt =
                paymentAttemptService.createAttempt(
                        request.transactionId(),
                        request.providerId()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(toResponse(attempt));
    }

    // Run a CREATED attempt through the gateway adapter (start + outcome).
    @PostMapping("/{attemptId}/process")
    public ResponseEntity<Map<String, Object>> processAttempt(
            @PathVariable Long attemptId) {

        return ResponseEntity.ok(
                toResponse(processingService.process(attemptId)));
    }

    // 2. Start processing an attempt.
    @PatchMapping("/{attemptId}/start")
    public ResponseEntity<Map<String, Object>> startAttempt(
            @PathVariable Long attemptId) {

        PaymentAttempt attempt =
                paymentAttemptService.startAttempt(attemptId);

        return ResponseEntity.ok(toResponse(attempt));
    }

    // 3. Mark an attempt successful.
    @PatchMapping("/{attemptId}/success")
    public ResponseEntity<Map<String, Object>> markSuccessful(
            @PathVariable Long attemptId,
            @RequestBody SuccessRequest request) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Request body is required."
            );
        }

        PaymentAttempt attempt =
                paymentAttemptService.markAttemptSuccessful(
                        attemptId,
                        request.providerTransactionReference()
                );

        return ResponseEntity.ok(toResponse(attempt));
    }

    // 4. Mark an attempt failed.
    @PatchMapping("/{attemptId}/failure")
    public ResponseEntity<Map<String, Object>> markFailed(
            @PathVariable Long attemptId,
            @RequestBody FailureRequest request) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Request body is required."
            );
        }

        PaymentAttempt attempt =
                paymentAttemptService.markAttemptFailed(
                        attemptId,
                        request.failureReason(),
                        request.failureCategory()
                );

        return ResponseEntity.ok(toResponse(attempt));
    }

    // 5. Retrieve one attempt.
    @GetMapping("/{attemptId}")
    public ResponseEntity<Map<String, Object>> getAttempt(
            @PathVariable Long attemptId) {

        PaymentAttempt attempt =
                paymentAttemptService.getAttempt(attemptId);

        return ResponseEntity.ok(toResponse(attempt));
    }

    // 6. Retrieve all attempts for a transaction.
    @GetMapping("/transaction/{transactionId}")
    public ResponseEntity<List<Map<String, Object>>> getAttemptsForTransaction(
            @PathVariable Long transactionId) {

        List<PaymentAttempt> attempts =
                paymentAttemptService.getAttemptsForTransaction(
                        transactionId
                );

        List<Map<String, Object>> response = attempts.stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(response);
    }

    // Convert the entity into a safe API response.
    private Map<String, Object> toResponse(PaymentAttempt attempt) {

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("id", attempt.getId());
        response.put(
                "transactionId",
                attempt.getTransaction().getId()
        );
        response.put(
                "providerId",
                attempt.getProvider().getId()
        );
        response.put(
                "providerName",
                attempt.getProvider().getProviderName()
        );
        response.put(
                "attemptNumber",
                attempt.getAttemptNumber()
        );
        response.put(
                "status",
                attempt.getStatus()
        );
        response.put(
                "providerTransactionReference",
                attempt.getProviderTransactionReference()
        );
        response.put(
                "failureReason",
                attempt.getFailureReason()
        );
        response.put(
                "failureCategory",
                attempt.getFailureCategory()
        );
        response.put("startedAt", attempt.getStartedAt());
        response.put("completedAt", attempt.getCompletedAt());
        response.put("createdAt", attempt.getCreatedAt());

        return response;
    }


    public record CreateAttemptRequest(
            Long transactionId,
            Long providerId) {
    }

    public record SuccessRequest(
            String providerTransactionReference) {
    }


    public record FailureRequest(
            String failureReason,
            PaymentFailureCategory failureCategory) {
    }
}
