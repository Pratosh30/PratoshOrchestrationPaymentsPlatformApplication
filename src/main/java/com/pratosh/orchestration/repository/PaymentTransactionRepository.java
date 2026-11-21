package com.pratosh.orchestration.repository;

import com.pratosh.orchestration.entity.PaymentTransaction;
import com.pratosh.orchestration.entity.PaymentMethod;
import com.pratosh.orchestration.entity.PaymentTransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import java.util.Optional;

public interface PaymentTransactionRepository
        extends JpaRepository<PaymentTransaction, Long> {

    /**
     * Finds a transaction using its unique transaction reference.
     */
    Optional<PaymentTransaction> findByTransactionReference(
            String transactionReference);

    /**
     * Finds a transaction using the merchant order ID.
     */
    Optional<PaymentTransaction> findByMerchantOrderId(
            String merchantOrderId);

    /**
     * Checks whether a transaction already exists
     * for the specified merchant order ID.
     */
    boolean existsByMerchantOrderId(String merchantOrderId);

    Optional<PaymentTransaction> findByIdempotencyKey(String idempotencyKey);

    List<PaymentTransaction> findByCreatedAtGreaterThanEqualOrderByIdAsc(
            LocalDateTime since);

    long countByStatus(PaymentTransactionStatus status);

    List<PaymentTransaction> findTop10ByOrderByIdDesc();

    List<PaymentTransaction> findByProviderTransactionReferenceIn(
            Collection<String> providerTransactionReferences);

    List<PaymentTransaction>
    findByProvider_IdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            Long providerId, LocalDateTime start, LocalDateTime end);

    @Query("select coalesce(sum(t.amount), 0) from PaymentTransaction t "
            + "where t.status = :status")
    BigDecimal sumAmountByStatus(
            @Param("status") PaymentTransactionStatus status);

    @Query("select t from PaymentTransaction t where "
            + "(:status is null or t.status = :status) and "
            + "(:providerId is null or t.provider.id = :providerId) and "
            + "(:method is null or t.paymentMethod = :method) and "
            + "(:start is null or t.createdAt >= :start) and "
            + "(:end is null or t.createdAt < :end)")
    Page<PaymentTransaction> search(
            @Param("status") PaymentTransactionStatus status,
            @Param("providerId") Long providerId,
            @Param("method") PaymentMethod method,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable);
}