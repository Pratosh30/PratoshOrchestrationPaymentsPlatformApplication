package com.pratosh.orchestration.service;

import com.pratosh.orchestration.entity.PaymentTransaction;
import com.pratosh.orchestration.entity.PaymentTransactionStatus;
import com.pratosh.orchestration.entity.ReconciliationRecord;
import com.pratosh.orchestration.entity.ReconciliationResult;
import com.pratosh.orchestration.service.ReconciliationService.ProviderReportRow;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ReconciliationServiceTest {

    private PaymentTransaction platform(PaymentTransactionStatus status,
                                        String amount) {
        PaymentTransaction t = new PaymentTransaction();
        t.setTransactionReference("PAY-1");
        t.setProviderTransactionReference("P-1");
        t.setStatus(status);
        t.setAmount(new BigDecimal(amount));
        t.setCurrency("INR");
        return t;
    }

    private ReconciliationRecord compare(String status, String amount,
                                         PaymentTransaction platform) {
        ProviderReportRow row = new ProviderReportRow(
                "P-1", status, new BigDecimal(amount), "INR");
        return ReconciliationService.compare("batch", 1L, "P-1", row, platform);
    }

    @Test
    void identicalRecordsMatch() {
        assertEquals(ReconciliationResult.MATCHED,
                compare("CAPTURED", "100.00",
                        platform(PaymentTransactionStatus.SUCCESS, "100"))
                        .getResult());
    }

    @Test
    void differentStatusIsAMismatch() {
        assertEquals(ReconciliationResult.STATUS_MISMATCH,
                compare("FAILED", "100",
                        platform(PaymentTransactionStatus.SUCCESS, "100"))
                        .getResult());
    }

    @Test
    void pendingOnPlatformSuccessAtProviderNeedsAnUpdate() {
        assertEquals(ReconciliationResult.STATUS_MISMATCH,
                compare("SUCCESS", "100",
                        platform(PaymentTransactionStatus.PROCESSING, "100"))
                        .getResult());
    }

    @Test
    void differentAmountIsFlagged() {
        assertEquals(ReconciliationResult.AMOUNT_MISMATCH,
                compare("SUCCESS", "99.00",
                        platform(PaymentTransactionStatus.SUCCESS, "100"))
                        .getResult());
    }

    @Test
    void unknownProviderReferenceIsMissingOnPlatform() {
        assertEquals(ReconciliationResult.MISSING_ON_PLATFORM,
                compare("SUCCESS", "100", null).getResult());
    }
}
