package com.pratosh.orchestration.service;

import com.pratosh.orchestration.dto.CreatePaymentRequest;
import com.pratosh.orchestration.dto.PaymentTransactionResponse;
import com.pratosh.orchestration.entity.PaymentAttempt;
import com.pratosh.orchestration.entity.PaymentTransactionStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class PaymentOrchestrationServiceTest {

    private PaymentTransactionService transactions;
    private PaymentAttemptService attempts;
    private PaymentProcessingService processing;
    private PaymentOrchestrationService service;
    private final CreatePaymentRequest request = new CreatePaymentRequest();

    private PaymentTransactionResponse response(
            PaymentTransactionStatus status, int attemptCount) {
        PaymentTransactionResponse r = mock(PaymentTransactionResponse.class);
        when(r.getId()).thenReturn(7L);
        when(r.getProviderId()).thenReturn(3L);
        when(r.getTransactionReference()).thenReturn("PAY-1");
        when(r.getStatus()).thenReturn(status);
        when(r.getAttemptCount()).thenReturn(attemptCount);
        return r;
    }

    @BeforeEach
    void setUp() {
        transactions = mock(PaymentTransactionService.class);
        attempts = mock(PaymentAttemptService.class);
        processing = mock(PaymentProcessingService.class);
        service = new PaymentOrchestrationService(
                transactions, attempts, processing);
        ReflectionTestUtils.setField(service, "autoProcess", true);
    }

    @Test
    void newPaymentGetsItsFirstAttemptProcessed() {
        PaymentTransactionResponse fresh =
                response(PaymentTransactionStatus.CREATED, 0);
        PaymentAttempt attempt = mock(PaymentAttempt.class);
        when(attempt.getId()).thenReturn(11L);
        when(transactions.createTransaction(request, "k")).thenReturn(fresh);
        when(attempts.createAttempt(7L, 3L)).thenReturn(attempt);
        when(transactions.getTransactionByReference("PAY-1"))
                .thenReturn(fresh);

        service.createAndProcess(request, "k");

        verify(processing).process(11L);
    }

    @Test
    void idempotentReplayDoesNotStartAnotherAttempt() {
        when(transactions.createTransaction(request, "k")).thenReturn(
                response(PaymentTransactionStatus.SUCCESS, 1));

        service.createAndProcess(request, "k");

        verify(attempts, never()).createAttempt(anyLong(), anyLong());
        verify(processing, never()).process(anyLong());
    }

    @Test
    void processingFailureDoesNotFailPaymentCreation() {
        PaymentTransactionResponse fresh =
                response(PaymentTransactionStatus.CREATED, 0);
        when(transactions.createTransaction(request, null)).thenReturn(fresh);
        when(attempts.createAttempt(7L, 3L))
                .thenThrow(new IllegalStateException("boom"));
        when(transactions.getTransactionByReference("PAY-1"))
                .thenReturn(fresh);

        service.createAndProcess(request, null); // must not throw
    }

    @Test
    void switchedOffKeepsManualFlow() {
        ReflectionTestUtils.setField(service, "autoProcess", false);
        when(transactions.createTransaction(request, null)).thenReturn(
                response(PaymentTransactionStatus.CREATED, 0));

        service.createAndProcess(request, null);

        verify(attempts, never()).createAttempt(anyLong(), anyLong());
    }
}
