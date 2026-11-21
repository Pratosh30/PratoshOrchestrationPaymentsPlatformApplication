package com.pratosh.orchestration.service;

import com.pratosh.orchestration.entity.PaymentAttempt;
import com.pratosh.orchestration.entity.PaymentAttemptStatus;
import com.pratosh.orchestration.entity.PaymentTransaction;
import com.pratosh.orchestration.entity.PaymentTransactionStatus;
import com.pratosh.orchestration.repository.PaymentAttemptRepository;
import com.pratosh.orchestration.repository.PaymentTransactionRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Live breakdowns of payment activity for the dashboard.
 *
 * Amounts are never added across currencies: every amount is reported per
 * currency. Success rate = SUCCESS / (SUCCESS + FAILED); payments still in
 * flight are reported separately.
 */
@Service
public class InsightsService {

    private final PaymentTransactionRepository transactionRepository;
    private final PaymentAttemptRepository attemptRepository;

    public InsightsService(
            PaymentTransactionRepository transactionRepository,
            PaymentAttemptRepository attemptRepository) {
        this.transactionRepository = transactionRepository;
        this.attemptRepository = attemptRepository;
    }

    /** Running totals for one group (a currency, provider, method, ...). */
    private static final class Bucket {
        long transactions;
        long success;
        long failed;
        long refunded;
        long pending;
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal successAmount = BigDecimal.ZERO;
        BigDecimal failedAmount = BigDecimal.ZERO;
        final Map<String, BigDecimal> successByCurrency = new TreeMap<>();

        void add(PaymentTransaction t) {
            BigDecimal amount =
                    t.getAmount() == null ? BigDecimal.ZERO : t.getAmount();
            PaymentTransactionStatus status = t.getStatus();

            transactions++;
            totalAmount = totalAmount.add(amount);

            if (status == PaymentTransactionStatus.SUCCESS) {
                success++;
                successAmount = successAmount.add(amount);
                successByCurrency.merge(
                        currencyOf(t), amount, BigDecimal::add);
            } else if (status == PaymentTransactionStatus.FAILED
                    || status == PaymentTransactionStatus.CANCELLED) {
                failed++;
                failedAmount = failedAmount.add(amount);
            } else if (status == PaymentTransactionStatus.REFUNDED
                    || status == PaymentTransactionStatus.REFUND_PENDING) {
                refunded++;
            } else {
                pending++;
            }
        }

        Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("transactions", transactions);
            map.put("success", success);
            map.put("failed", failed);
            map.put("refunded", refunded);
            map.put("pending", pending);
            map.put("successRatePercent", rate(success, failed));
            map.put("totalAmount", totalAmount);
            map.put("successAmount", successAmount);
            map.put("failedAmount", failedAmount);
            map.put("averageAmount", transactions == 0
                    ? BigDecimal.ZERO
                    : totalAmount.divide(
                    BigDecimal.valueOf(transactions), 2,
                    RoundingMode.HALF_UP));
            map.put("successAmountByCurrency", successByCurrency);
            return map;
        }
    }

    private static String currencyOf(PaymentTransaction t) {
        return t.getCurrency() == null ? "N/A" : t.getCurrency();
    }

    static BigDecimal rate(long success, long failed) {
        long finished = success + failed;
        return finished == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(success * 100L)
                .divide(BigDecimal.valueOf(finished), 1,
                        RoundingMode.HALF_UP);
    }

    private static List<Map<String, Object>> toSortedList(
            Map<String, Bucket> groups, String keyName) {

        List<Map<String, Object>> list = new ArrayList<>();
        for (Map.Entry<String, Bucket> entry : groups.entrySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put(keyName, entry.getKey());
            row.putAll(entry.getValue().toMap());
            list.add(row);
        }
        list.sort(Comparator.comparing(
                (Map<String, Object> row) ->
                        (Long) row.get("transactions")).reversed());
        return list;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> insights(int days) {

        int window = Math.min(Math.max(days, 1), 90);
        LocalDate today = LocalDate.now();
        LocalDateTime since = today.minusDays(window - 1L).atStartOfDay();

        List<PaymentTransaction> transactions = transactionRepository
                .findByCreatedAtGreaterThanEqualOrderByIdAsc(since);
        List<PaymentAttempt> attempts =
                attemptRepository.findByCreatedAtGreaterThanEqual(since);

        Bucket totals = new Bucket();
        Map<String, Bucket> byCurrency = new LinkedHashMap<>();
        Map<String, Bucket> byProvider = new LinkedHashMap<>();
        Map<String, Bucket> byMethod = new LinkedHashMap<>();

        // [transactions, success, failed] for every day in the window.
        Map<LocalDate, long[]> daily = new TreeMap<>();
        for (LocalDate d = since.toLocalDate();
             !d.isAfter(today); d = d.plusDays(1)) {
            daily.put(d, new long[3]);
        }

        long retriedTransactions = 0;
        long attemptsOnTransactions = 0;
        long transactionsWithAttempts = 0;

        for (PaymentTransaction t : transactions) {
            totals.add(t);
            byCurrency.computeIfAbsent(currencyOf(t),
                    k -> new Bucket()).add(t);

            String provider = t.getProvider() == null
                    ? "Unassigned"
                    : t.getProvider().getProviderName();
            byProvider.computeIfAbsent(provider,
                    k -> new Bucket()).add(t);

            byMethod.computeIfAbsent(String.valueOf(t.getPaymentMethod()),
                    k -> new Bucket()).add(t);

            if (t.getCreatedAt() != null) {
                long[] row = daily.get(t.getCreatedAt().toLocalDate());
                if (row != null) {
                    row[0]++;
                    if (t.getStatus() == PaymentTransactionStatus.SUCCESS) {
                        row[1]++;
                    } else if (t.getStatus()
                            == PaymentTransactionStatus.FAILED
                            || t.getStatus()
                            == PaymentTransactionStatus.CANCELLED) {
                        row[2]++;
                    }
                }
            }

            int attemptCount = t.getAttemptCount();
            if (attemptCount > 0) {
                transactionsWithAttempts++;
                attemptsOnTransactions += attemptCount;
            }
            if (attemptCount > 1) {
                retriedTransactions++;
            }
        }

        List<Map<String, Object>> dailyList = new ArrayList<>();
        for (Map.Entry<LocalDate, long[]> entry : daily.entrySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", entry.getKey().toString());
            row.put("transactions", entry.getValue()[0]);
            row.put("success", entry.getValue()[1]);
            row.put("failed", entry.getValue()[2]);
            dailyList.add(row);
        }

        long attemptSuccess = 0;
        long attemptFailed = 0;
        long durationMs = 0;
        long durationCount = 0;
        Map<String, Long> categories = new TreeMap<>();

        for (PaymentAttempt a : attempts) {
            if (a.getStatus() == PaymentAttemptStatus.SUCCESS) {
                attemptSuccess++;
            } else if (a.getStatus() == PaymentAttemptStatus.FAILED) {
                attemptFailed++;
            }
            if (a.getStartedAt() != null && a.getCompletedAt() != null) {
                durationMs += Duration.between(
                        a.getStartedAt(), a.getCompletedAt()).toMillis();
                durationCount++;
            }
            if (a.getFailureCategory() != null) {
                categories.merge(a.getFailureCategory().name(), 1L,
                        Long::sum);
            }
        }

        long totalFailures = 0;
        for (long count : categories.values()) {
            totalFailures += count;
        }

        List<Map<String, Object>> failureList = new ArrayList<>();
        for (Map.Entry<String, Long> entry : categories.entrySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("category", entry.getKey());
            row.put("count", entry.getValue());
            row.put("sharePercent", BigDecimal.valueOf(
                            entry.getValue() * 100L)
                    .divide(BigDecimal.valueOf(totalFailures), 1,
                            RoundingMode.HALF_UP));
            failureList.add(row);
        }
        failureList.sort(Comparator.comparing(
                (Map<String, Object> row) ->
                        (Long) row.get("count")).reversed());

        Map<String, Object> attemptStats = new LinkedHashMap<>();
        attemptStats.put("total", (long) attempts.size());
        attemptStats.put("success", attemptSuccess);
        attemptStats.put("failed", attemptFailed);
        attemptStats.put("retriedTransactions", retriedTransactions);
        attemptStats.put("averageAttemptsPerTransaction",
                transactionsWithAttempts == 0
                        ? BigDecimal.ZERO
                        : BigDecimal.valueOf(attemptsOnTransactions)
                        .divide(BigDecimal.valueOf(transactionsWithAttempts),
                                2, RoundingMode.HALF_UP));
        attemptStats.put("averageProcessingMs",
                durationCount == 0 ? 0L : durationMs / durationCount);
        attemptStats.put("failureCategories", failureList);

        long allSuccess = transactionRepository
                .countByStatus(PaymentTransactionStatus.SUCCESS);
        long allFailed = transactionRepository
                .countByStatus(PaymentTransactionStatus.FAILED);

        Map<String, Object> allTime = new LinkedHashMap<>();
        allTime.put("transactions", transactionRepository.count());
        allTime.put("success", allSuccess);
        allTime.put("failed", allFailed);
        allTime.put("successRatePercent", rate(allSuccess, allFailed));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("generatedAt", LocalDateTime.now().toString());
        body.put("windowDays", window);
        body.put("allTime", allTime);
        body.put("totals", totals.toMap());
        body.put("byCurrency", toSortedList(byCurrency, "currency"));
        body.put("byProvider", toSortedList(byProvider, "provider"));
        body.put("byPaymentMethod", toSortedList(byMethod, "paymentMethod"));
        body.put("daily", dailyList);
        body.put("attempts", attemptStats);
        return body;
    }
}
