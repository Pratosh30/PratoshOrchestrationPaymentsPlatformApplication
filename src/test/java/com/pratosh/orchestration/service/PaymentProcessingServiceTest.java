package com.pratosh.orchestration.service;

import com.pratosh.orchestration.entity.PaymentAttempt;
import com.pratosh.orchestration.entity.PaymentFailureCategory;
import com.pratosh.orchestration.entity.PaymentProvider;
import com.pratosh.orchestration.entity.PaymentTransaction;
import com.pratosh.orchestration.gateway.GatewayResult;
import com.pratosh.orchestration.gateway.PaymentGateway;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentProcessingServiceTest {

    private PaymentAttemptService attemptService;
    private PaymentGateway gateway;
    private PaymentProcessingService service;
    private PaymentAttempt attempt;

    @BeforeEach
    void setUp() {
        attemptService = mock(PaymentAttemptService.class);
        gateway = mock(PaymentGateway.class);
        service = new PaymentProcessingService(attemptService, gateway);

        attempt = new PaymentAttempt();
        attempt.setTransaction(new PaymentTransaction());
        attempt.setProvider(new PaymentProvider());
        when(attemptService.startAttempt(1L)).thenReturn(attempt);
    }

    @Test
    void successfulGatewayResultMarksTheAttemptSuccessful() {
        when(gateway.process(any(), any()))
                .thenReturn(GatewayResult.success("REF-1"));

        service.process(1L);

        verify(attemptService).markAttemptSuccessful(1L, "REF-1");
    }

    @Test
    void failureIsRecordedWithItsCategory() {
        when(gateway.process(any(), any())).thenReturn(
                GatewayResult.failure("declined",
                        PaymentFailureCategory.PAYMENT_DECLINED));

        service.process(1L);

        verify(attemptService).markAttemptFailed(
                1L, "declined", PaymentFailureCategory.PAYMENT_DECLINED);
    }

    @Test
    void gatewayExceptionIsTreatedAsAnUncertainOutcome() {
        when(gateway.process(any(), any()))
                .thenThrow(new RuntimeException("boom"));

        service.process(1L);

        verify(attemptService).markAttemptFailed(
                eq(1L), any(), eq(PaymentFailureCategory.UNKNOWN));
    }
}
