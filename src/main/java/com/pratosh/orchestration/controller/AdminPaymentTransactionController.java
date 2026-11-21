package com.pratosh.orchestration.controller;

import com.pratosh.orchestration.dto.CreatePaymentRequest;
import com.pratosh.orchestration.dto.PaymentTransactionResponse;
import com.pratosh.orchestration.entity.PaymentMethod;
import com.pratosh.orchestration.entity.PaymentTransactionStatus;
import com.pratosh.orchestration.service.PaymentOrchestrationService;
import com.pratosh.orchestration.service.PaymentTransactionService;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Admin-facing transaction endpoints used by the dashboard.
 * The public API under /api/v1/payments is unchanged.
 */
@RestController
@RequestMapping("/api/v1/admin/payment-transactions")
public class AdminPaymentTransactionController {

    private final PaymentTransactionService transactionService;
    private final PaymentOrchestrationService orchestrationService;

    public AdminPaymentTransactionController(
            PaymentTransactionService transactionService,
            PaymentOrchestrationService orchestrationService) {
        this.transactionService = transactionService;
        this.orchestrationService = orchestrationService;
    }

    /**
     * GET /api/v1/admin/payment-transactions
     *
     * Optional filters: status, providerId, paymentMethod, from, to
     * (yyyy-MM-dd, inclusive). Pagination: page (0-based), size (max 500).
     * The body is a plain array; the total is in the X-Total-Count header.
     */
    @GetMapping
    public ResponseEntity<List<PaymentTransactionResponse>> list(
            @RequestParam(required = false)
            PaymentTransactionStatus status,
            @RequestParam(required = false) Long providerId,
            @RequestParam(required = false) PaymentMethod paymentMethod,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {

        Page<PaymentTransactionResponse> result =
                transactionService.search(
                        status, providerId, paymentMethod,
                        from, to, page, size);

        return ResponseEntity.ok()
                .header("X-Total-Count",
                        String.valueOf(result.getTotalElements()))
                .body(result.getContent());
    }

    // GET /api/v1/admin/payment-transactions/{transactionReference}
    @GetMapping("/{transactionReference}")
    public ResponseEntity<PaymentTransactionResponse> get(
            @PathVariable String transactionReference) {
        return ResponseEntity.ok(
                transactionService.getTransactionByReference(
                        transactionReference));
    }

    // POST /api/v1/admin/payment-transactions
    @PostMapping
    public ResponseEntity<PaymentTransactionResponse> create(
            @RequestHeader(value = "Idempotency-Key", required = false)
            String idempotencyKey,
            @Valid @RequestBody CreatePaymentRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(orchestrationService.createAndProcess(
                        request, idempotencyKey));
    }
}
