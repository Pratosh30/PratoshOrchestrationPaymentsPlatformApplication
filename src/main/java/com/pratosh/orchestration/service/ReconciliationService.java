package com.pratosh.orchestration.service;

import com.pratosh.orchestration.entity.PaymentTransaction;
import com.pratosh.orchestration.entity.PaymentTransactionStatus;
import com.pratosh.orchestration.entity.ReconciliationRecord;
import com.pratosh.orchestration.entity.ReconciliationResult;
import com.pratosh.orchestration.repository.PaymentProviderRepository;
import com.pratosh.orchestration.repository.PaymentTransactionRepository;
import com.pratosh.orchestration.repository.ReconciliationRecordRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Compares the platform's transactions with a provider's report.
 *
 * Matching key: the provider transaction reference. The platform stores it
 * only for successful payments, so a failed or pending line in the provider
 * report with no platform match is reported as MISSING_ON_PLATFORM with an
 * explanatory note rather than treated as certain loss.
 */
@Service
public class ReconciliationService {

    private static final int MAX_ROWS = 10_000;

    /** One line of a provider settlement/transaction report. */
    public record ProviderReportRow(
            String providerTransactionReference,
            String status,
            BigDecimal amount,
            String currency) {
    }

    public record Summary(
            String batchId,
            int total,
            Map<ReconciliationResult, Integer> counts,
            List<ReconciliationRecord> records) {
    }

    private final PaymentTransactionRepository transactionRepository;
    private final PaymentProviderRepository providerRepository;
    private final ReconciliationRecordRepository recordRepository;

    public ReconciliationService(
            PaymentTransactionRepository transactionRepository,
            PaymentProviderRepository providerRepository,
            ReconciliationRecordRepository recordRepository) {
        this.transactionRepository = transactionRepository;
        this.providerRepository = providerRepository;
        this.recordRepository = recordRepository;
    }

    @Transactional
    public Summary run(Long providerId,
                       LocalDate from,
                       LocalDate to,
                       List<ProviderReportRow> rows) {

        if (providerId == null || from == null || to == null
                || rows == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "providerId, from, to and rows are required.");
        }
        if (to.isBefore(from)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "'to' must not be before 'from'.");
        }
        if (rows.size() > MAX_ROWS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A report can contain at most " + MAX_ROWS + " rows.");
        }
        if (!providerRepository.existsById(providerId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Payment provider not found");
        }

        List<String> references = new ArrayList<>();
        for (ProviderReportRow row : rows) {
            if (row == null || row.providerTransactionReference() == null
                    || row.providerTransactionReference().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Every row needs a providerTransactionReference.");
            }
            references.add(row.providerTransactionReference().trim());
        }

        Map<String, PaymentTransaction> byReference = new HashMap<>();
        if (!references.isEmpty()) {
            for (PaymentTransaction t : transactionRepository
                    .findByProviderTransactionReferenceIn(references)) {
                byReference.put(t.getProviderTransactionReference(), t);
            }
        }

        String batchId = UUID.randomUUID().toString();
        List<ReconciliationRecord> records = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (ProviderReportRow row : rows) {
            String ref = row.providerTransactionReference().trim();
            seen.add(ref);
            records.add(compare(batchId, providerId, ref, row,
                    byReference.get(ref)));
        }

        // Platform successes in the period that the provider did not report.
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();

        for (PaymentTransaction t : transactionRepository
                .findByProvider_IdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                        providerId, start, end)) {

            if (t.getStatus() == PaymentTransactionStatus.SUCCESS
                    && t.getProviderTransactionReference() != null
                    && !seen.contains(t.getProviderTransactionReference())) {

                ReconciliationRecord r = base(batchId, providerId);
                r.setTransactionReference(t.getTransactionReference());
                r.setProviderTransactionReference(
                        t.getProviderTransactionReference());
                r.setPlatformStatus("SUCCESS");
                r.setPlatformAmount(t.getAmount());
                r.setResult(ReconciliationResult.MISSING_AT_PROVIDER);
                r.setNotes("Successful on the platform but absent from "
                        + "the provider report.");
                records.add(r);
            }
        }

        List<ReconciliationRecord> saved = recordRepository.saveAll(records);

        Map<ReconciliationResult, Integer> counts =
                new EnumMap<>(ReconciliationResult.class);
        for (ReconciliationResult result : ReconciliationResult.values()) {
            counts.put(result, 0);
        }
        for (ReconciliationRecord r : saved) {
            counts.merge(r.getResult(), 1, Integer::sum);
        }

        return new Summary(batchId, saved.size(), counts, saved);
    }

    @Transactional(readOnly = true)
    public Summary getBatch(String batchId) {
        List<ReconciliationRecord> records =
                recordRepository.findByBatchIdOrderByIdAsc(batchId);

        if (records.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Reconciliation batch not found");
        }

        Map<ReconciliationResult, Integer> counts =
                new EnumMap<>(ReconciliationResult.class);
        for (ReconciliationResult result : ReconciliationResult.values()) {
            counts.put(result, 0);
        }
        for (ReconciliationRecord r : records) {
            counts.merge(r.getResult(), 1, Integer::sum);
        }

        return new Summary(batchId, records.size(), counts, records);
    }

    /** Open (unreviewed) discrepancies across all batches. */
    @Transactional(readOnly = true)
    public List<ReconciliationRecord> openDiscrepancies() {
        return recordRepository.findByResultNotAndReviewedOrderByIdDesc(
                ReconciliationResult.MATCHED, false);
    }

    @Transactional
    public ReconciliationRecord review(Long id, String note) {
        ReconciliationRecord record = recordRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Reconciliation record not found"));

        if (note == null || note.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A review note is required.");
        }
        if (note.length() > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The review note cannot exceed 500 characters.");
        }

        record.setReviewed(true);
        record.setReviewNote(note.trim());
        record.setReviewedAt(LocalDateTime.now());
        return recordRepository.save(record);
    }

    // ---- helpers -------------------------------------------------------

    static ReconciliationRecord compare(String batchId,
                                        Long providerId,
                                        String ref,
                                        ProviderReportRow row,
                                        PaymentTransaction platform) {

        ReconciliationRecord r = base(batchId, providerId);
        r.setProviderTransactionReference(ref);
        String providerStatus = normalizeProviderStatus(row.status());
        r.setProviderStatus(providerStatus);
        r.setProviderAmount(row.amount());

        if (platform == null) {
            r.setResult(ReconciliationResult.MISSING_ON_PLATFORM);
            r.setNotes("No platform transaction has this provider "
                    + "reference."
                    + ("SUCCESS".equals(providerStatus) ? ""
                    : " The platform records the provider reference only "
                    + "for successful payments."));
            return r;
        }

        String platformStatus = normalizePlatformStatus(platform.getStatus());
        r.setTransactionReference(platform.getTransactionReference());
        r.setPlatformStatus(platformStatus);
        r.setPlatformAmount(platform.getAmount());

        if (!platformStatus.equals(providerStatus)) {
            r.setResult(ReconciliationResult.STATUS_MISMATCH);
            r.setNotes("Platform " + platformStatus
                    + " but provider " + providerStatus + ".");
        } else if (!sameAmount(platform.getAmount(), row.amount())
                || !sameCurrency(platform.getCurrency(), row.currency())) {
            r.setResult(ReconciliationResult.AMOUNT_MISMATCH);
            r.setNotes("Amount or currency differs from the provider.");
        } else {
            r.setResult(ReconciliationResult.MATCHED);
        }
        return r;
    }

    private static ReconciliationRecord base(String batchId, Long providerId) {
        ReconciliationRecord r = new ReconciliationRecord();
        r.setBatchId(batchId);
        r.setProviderId(providerId);
        return r;
    }

    static String normalizeProviderStatus(String status) {
        String s = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        return switch (s) {
            case "SUCCESS", "SUCCESSFUL", "CAPTURED", "PAID", "SETTLED" -> "SUCCESS";
            case "FAILED", "FAILURE", "DECLINED", "CANCELLED" -> "FAILED";
            default -> "PENDING";
        };
    }

    static String normalizePlatformStatus(PaymentTransactionStatus status) {
        if (status == PaymentTransactionStatus.SUCCESS) {
            return "SUCCESS";
        }
        if (status == PaymentTransactionStatus.FAILED
                || status == PaymentTransactionStatus.CANCELLED) {
            return "FAILED";
        }
        return "PENDING";
    }

    private static boolean sameAmount(BigDecimal a, BigDecimal b) {
        return a != null && b != null && a.compareTo(b) == 0;
    }

    private static boolean sameCurrency(String a, String b) {
        // A report without a currency column is not treated as a mismatch.
        return b == null || b.isBlank() || (a != null && a.equalsIgnoreCase(b.trim()));
    }
}
