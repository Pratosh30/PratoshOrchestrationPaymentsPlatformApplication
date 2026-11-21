package com.pratosh.orchestration.entity;

/**
 * Represents the lifecycle states of a payment transaction.
 *
 * These states help the orchestration platform track
 * payments from creation through final resolution.
 */
public enum PaymentTransactionStatus {

    /**
     * The transaction has been created but processing
     * has not started.
     */
    CREATED,

    /**
     * The transaction is currently being processed
     * by the orchestration platform or gateway adapter.
     */
    PROCESSING,

    /**
     * The payment provider has confirmed successful payment.
     */
    SUCCESS,

    /**
     * The payment attempt failed.
     */
    FAILED,

    /**
     * The payment outcome is unknown, for example,
     * because the gateway timed out after receiving
     * the payment request.
     */
    UNKNOWN,

    /**
     * A refund has been requested.
     */
    REFUND_PENDING,

    /**
     * The payment has been refunded successfully.
     */
    REFUNDED,

    /**
     * The transaction was cancelled before completion.
     */
    CANCELLED
}