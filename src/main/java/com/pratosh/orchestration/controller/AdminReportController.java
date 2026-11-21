package com.pratosh.orchestration.controller;

import com.pratosh.orchestration.dto.PaymentTransactionResponse;
import com.pratosh.orchestration.entity.PaymentMethod;
import com.pratosh.orchestration.entity.PaymentTransactionStatus;
import com.pratosh.orchestration.repository.PaymentProviderRepository;
import com.pratosh.orchestration.repository.PaymentTransactionRepository;
import com.pratosh.orchestration.service.PaymentTransactionService;

import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Dashboard summary and CSV export, computed from real transaction data.
 */
@RestController
@RequestMapping("/api/v1/admin/reports")
public class AdminReportController {

    private static final int EXPORT_PAGE_SIZE = 500;
    private static final int EXPORT_MAX_PAGES = 20; // 10,000 rows

    private final PaymentTransactionRepository transactionRepository;
    private final PaymentProviderRepository providerRepository;
    private final PaymentTransactionService transactionService;
    private final com.pratosh.orchestration.service.InsightsService
            insightsService;

    public AdminReportController(
            PaymentTransactionRepository transactionRepository,
            PaymentProviderRepository providerRepository,
            PaymentTransactionService transactionService,
            com.pratosh.orchestration.service.InsightsService
                    insightsService) {
        this.transactionRepository = transactionRepository;
        this.providerRepository = providerRepository;
        this.transactionService = transactionService;
        this.insightsService = insightsService;
    }

    /**
     * GET /api/v1/admin/reports/insights?days=7
     *
     * Live breakdown by currency, provider, payment method, day and
     * failure category. days is clamped to 1..90.
     */
    @GetMapping("/insights")
    public Map<String, Object> insights(
            @RequestParam(defaultValue = "7") int days) {
        return insightsService.insights(days);
    }

    /**
     * GET /api/v1/admin/reports/summary
     *
     * Success rate = SUCCESS / (SUCCESS + FAILED). Transactions that are
     * still in flight or have an uncertain outcome are reported separately
     * and excluded from the rate.
     */
    @GetMapping("/summary")
    @Transactional(readOnly = true)
    public Map<String, Object> summary() {

        long success = transactionRepository
                .countByStatus(PaymentTransactionStatus.SUCCESS);
        long failed = transactionRepository
                .countByStatus(PaymentTransactionStatus.FAILED);
        long created = transactionRepository
                .countByStatus(PaymentTransactionStatus.CREATED);
        long processing = transactionRepository
                .countByStatus(PaymentTransactionStatus.PROCESSING);
        long unknown = transactionRepository
                .countByStatus(PaymentTransactionStatus.UNKNOWN);

        long finished = success + failed;
        BigDecimal successRate = finished == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(success * 100L)
                .divide(BigDecimal.valueOf(finished), 2,
                        RoundingMode.HALF_UP);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("totalTransactions", transactionRepository.count());
        body.put("successful", success);
        body.put("failed", failed);
        body.put("pending", created + processing);
        body.put("unknown", unknown);
        body.put("successRatePercent", successRate);
        body.put("successfulAmount", transactionRepository
                .sumAmountByStatus(PaymentTransactionStatus.SUCCESS));
        body.put("enabledProviders",
                providerRepository.findByEnabled(true).size());
        body.put("recentTransactions",
                transactionRepository.findTop10ByOrderByIdDesc()
                        .stream()
                        .map(PaymentTransactionResponse::fromEntity)
                        .toList());

        return body;
    }

    /**
     * GET /api/v1/admin/reports/transactions.csv
     * Same filters as the transaction list. Capped at 10,000 rows.
     */
    @GetMapping("/transactions.csv")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam(required = false)
            PaymentTransactionStatus status,
            @RequestParam(required = false) Long providerId,
            @RequestParam(required = false) PaymentMethod paymentMethod,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        StringBuilder csv = new StringBuilder(
                "id,transactionReference,merchantOrderId,amount,currency,"
                        + "paymentMethod,status,provider,"
                        + "providerTransactionReference,attemptCount,"
                        + "createdAt\r\n");

        for (int page = 0; page < EXPORT_MAX_PAGES; page++) {
            Page<PaymentTransactionResponse> result =
                    transactionService.search(
                            status, providerId, paymentMethod,
                            from, to, page, EXPORT_PAGE_SIZE);

            for (PaymentTransactionResponse t : result.getContent()) {
                csv.append(cell(t.getId())).append(',')
                        .append(cell(t.getTransactionReference())).append(',')
                        .append(cell(t.getMerchantOrderId())).append(',')
                        .append(cell(t.getAmount())).append(',')
                        .append(cell(t.getCurrency())).append(',')
                        .append(cell(t.getPaymentMethod())).append(',')
                        .append(cell(t.getStatus())).append(',')
                        .append(cell(t.getProviderName())).append(',')
                        .append(cell(t.getProviderTransactionReference()))
                        .append(',')
                        .append(cell(t.getAttemptCount())).append(',')
                        .append(cell(t.getCreatedAt())).append("\r\n");
            }

            if (!result.hasNext()) {
                break;
            }
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"transactions.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Quotes a CSV field and neutralises spreadsheet formula injection:
     * values such as merchant order IDs are caller-controlled.
     */
    static String cell(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value);
        if (!text.isEmpty() && "=+-@\t\r".indexOf(text.charAt(0)) >= 0) {
            text = "'" + text;
        }
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }
}
