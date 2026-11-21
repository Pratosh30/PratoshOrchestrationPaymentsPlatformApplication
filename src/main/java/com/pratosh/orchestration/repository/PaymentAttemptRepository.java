package com.pratosh.orchestration.repository;

import com.pratosh.orchestration.entity.PaymentAttempt;
import com.pratosh.orchestration.entity.PaymentAttemptStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentAttemptRepository
        extends JpaRepository<PaymentAttempt, Long> {

    // Retrieve all attempts for a particular transaction.
    List<PaymentAttempt> findByTransaction_IdOrderByAttemptNumberAsc(
            Long transactionId
    );

    // Retrieve the latest attempt for a transaction.
    Optional<PaymentAttempt>
    findFirstByTransaction_IdOrderByAttemptNumberDesc(
            Long transactionId
    );

    // Count attempts associated with a transaction.
    long countByTransaction_Id(Long transactionId);

    // Retrieve attempts having a particular status.
    List<PaymentAttempt> findByStatus(
            PaymentAttemptStatus status
    );

    List<PaymentAttempt> findByCreatedAtGreaterThanEqual(
            java.time.LocalDateTime since);

    // Failed attempts whose transaction is waiting for a retry.
    List<PaymentAttempt> findByStatusAndTransaction_Status(
            PaymentAttemptStatus status,
            com.pratosh.orchestration.entity.PaymentTransactionStatus
                    transactionStatus
    );

    // Check whether an attempt number already exists for a transaction.
    boolean existsByTransaction_IdAndAttemptNumber(
            Long transactionId,
            Integer attemptNumber
    );
}
