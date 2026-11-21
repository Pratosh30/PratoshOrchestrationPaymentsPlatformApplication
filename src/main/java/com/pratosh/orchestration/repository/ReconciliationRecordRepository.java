package com.pratosh.orchestration.repository;

import com.pratosh.orchestration.entity.ReconciliationRecord;
import com.pratosh.orchestration.entity.ReconciliationResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReconciliationRecordRepository
        extends JpaRepository<ReconciliationRecord, Long> {

    List<ReconciliationRecord> findByBatchIdOrderByIdAsc(String batchId);

    List<ReconciliationRecord> findByResultNotAndReviewedOrderByIdDesc(
            ReconciliationResult result, boolean reviewed);
}
