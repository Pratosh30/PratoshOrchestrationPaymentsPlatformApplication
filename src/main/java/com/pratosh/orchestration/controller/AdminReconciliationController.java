package com.pratosh.orchestration.controller;

import com.pratosh.orchestration.entity.ReconciliationRecord;
import com.pratosh.orchestration.service.ReconciliationService;
import com.pratosh.orchestration.service.ReconciliationService.ProviderReportRow;
import com.pratosh.orchestration.service.ReconciliationService.Summary;

import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/reconciliation")
public class AdminReconciliationController {

    public record RunRequest(
            Long providerId,
            LocalDate from,
            LocalDate to,
            List<ProviderReportRow> rows) {
    }

    public record ReviewRequest(String note) {
    }

    private final ReconciliationService reconciliationService;

    public AdminReconciliationController(
            ReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    // POST /api/v1/admin/reconciliation/run
    @PostMapping("/run")
    public Summary run(@RequestBody RunRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required.");
        }
        return reconciliationService.run(
                request.providerId(), request.from(), request.to(),
                request.rows());
    }

    // GET /api/v1/admin/reconciliation/batches/{batchId}
    @GetMapping("/batches/{batchId}")
    public Summary batch(@PathVariable String batchId) {
        return reconciliationService.getBatch(batchId);
    }

    // GET /api/v1/admin/reconciliation/discrepancies
    @GetMapping("/discrepancies")
    public List<ReconciliationRecord> discrepancies() {
        return reconciliationService.openDiscrepancies();
    }

    // PATCH /api/v1/admin/reconciliation/records/{id}/review
    @PatchMapping("/records/{id}/review")
    public ReconciliationRecord review(
            @PathVariable Long id,
            @RequestBody ReviewRequest request) {
        return reconciliationService.review(
                id, request == null ? null : request.note());
    }
}
