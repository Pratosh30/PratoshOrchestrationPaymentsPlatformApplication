package com.pratosh.orchestration.repository;

import com.pratosh.orchestration.entity.PaymentMethod;
import com.pratosh.orchestration.entity.PaymentMethodConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;


 //Repository for managing payment method configurations.
 //Spring Data JPA automatically implements these methods. We do not need to write SQL queries for these operations.

public interface PaymentMethodConfigurationRepository
        extends JpaRepository<PaymentMethodConfiguration, Long> {

    /**
     * Retrieves all payment method configurations associated with a particular payment provider.
     * Example: Find every configured payment method for Razorpay.
     */
    List<PaymentMethodConfiguration> findByProvider_Id(Long providerId);

    /**
     * Finds a configuration for a particular provider and payment method.
     * Example: Find Razorpay's UPI configuration.
     */
    Optional<PaymentMethodConfiguration> findByProvider_IdAndPaymentMethod(
            Long providerId,
            PaymentMethod paymentMethod
    );

    /**
     * Checks whether the specified payment method has already been configured for the specified provider.
     * Useful when preventing duplicate configurations.
     */
    boolean existsByProvider_IdAndPaymentMethod(
            Long providerId,
            PaymentMethod paymentMethod
    );

    /**
     * Retrieves configurations that are enabled.
     * Useful when determining which methods are available for routing a payment.
     */
    List<PaymentMethodConfiguration> findByEnabledTrue();


      //Retrieves enabled configurations for a particular provider.

    List<PaymentMethodConfiguration> findByProvider_IdAndEnabledTrue(
            Long providerId
    );

    //Finds a configuration by its database ID.
    Optional<PaymentMethodConfiguration> findById(Long id);
}