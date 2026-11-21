package com.pratosh.orchestration.entity;

public enum ReconciliationResult {
    MATCHED,
    STATUS_MISMATCH,
    AMOUNT_MISMATCH,
    MISSING_AT_PROVIDER,
    MISSING_ON_PLATFORM
}
