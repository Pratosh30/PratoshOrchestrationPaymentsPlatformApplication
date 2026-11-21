package com.pratosh.orchestration.controller;

import com.pratosh.orchestration.dto.CreatePaymentRequest;
import com.pratosh.orchestration.dto.PaymentTransactionResponse;
import com.pratosh.orchestration.service.PaymentOrchestrationService;
import com.pratosh.orchestration.service.PaymentTransactionService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentTransactionController {

    private final PaymentTransactionService paymentTransactionService;
    private final PaymentOrchestrationService orchestrationService;

    public PaymentTransactionController(
            PaymentTransactionService paymentTransactionService,
            PaymentOrchestrationService orchestrationService) {
        this.paymentTransactionService = paymentTransactionService;
        this.orchestrationService = orchestrationService;
    }

    @PostMapping
    public ResponseEntity<PaymentTransactionResponse> createPayment(
            @RequestHeader(value = "Idempotency-Key", required = false)
            String idempotencyKey,
            @Valid @RequestBody CreatePaymentRequest request) {

        PaymentTransactionResponse response =
                orchestrationService.createAndProcess(
                        request, idempotencyKey);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }


    @GetMapping("/{transactionReference}")
    public ResponseEntity<PaymentTransactionResponse> getPayment(
            @PathVariable String transactionReference) {

        PaymentTransactionResponse response =
                paymentTransactionService
                        .getTransactionByReference(
                                transactionReference);

        return ResponseEntity.ok(response);
    }

}
