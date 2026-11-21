package com.pratosh.orchestration.repository;

import com.pratosh.orchestration.entity.PaymentProvider;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for accessing payment provider records in MySQL.
 *
 * Spring Data JPA automatically implements the database operations
 * declared by this interface.
 */
@Repository
public interface PaymentProviderRepository
        extends JpaRepository<PaymentProvider, Long> {

    /**
     * Find a provider using its unique provider code.
     *
     * Example: findByProviderCode("RAZORPAY")
     */
    Optional<PaymentProvider> findByProviderCode(String providerCode);

    /**
     * Check whether a provider code is already registered.
     *
     * Useful when preventing duplicate provider configurations.
     */
    boolean existsByProviderCode(String providerCode);

    /**
     * Retrieve all providers with the specified enabled status.
     *
     * Example: findByEnabled(true)
     */
    List<PaymentProvider> findByEnabled(boolean enabled);

    /**
     * Retrieve providers configured for a particular environment.
     *
     * Example: findByEnvironment("SANDBOX")
     */
    List<PaymentProvider> findByEnvironment(String environment);

    /**
     * Retrieve providers that are enabled in a particular environment.
     *
     * Example: findByEnabledAndEnvironment(true, "SANDBOX")
     */
    List<PaymentProvider> findByEnabledAndEnvironment(
            boolean enabled,
            String environment
    );
}
