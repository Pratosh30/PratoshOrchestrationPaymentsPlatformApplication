package com.pratosh.orchestration.service;

import com.pratosh.orchestration.entity.PaymentProvider;
import com.pratosh.orchestration.repository.PaymentProviderRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

/**
 * Contains the business logic for managing payment providers.
 *
 * This service sits between the REST controller and the repository.
 * It validates requests and coordinates database operations.
 */
@Service
@Transactional(readOnly = true)
public class PaymentProviderService {

    private final PaymentProviderRepository paymentProviderRepository;

    // Constructor injection provides the repository dependency.
    public PaymentProviderService(
            PaymentProviderRepository paymentProviderRepository) {
        this.paymentProviderRepository = paymentProviderRepository;
    }

    /**
     * Registers a new payment provider.
     *
     * Duplicate provider codes are rejected before saving.
     */
    @Transactional
    public PaymentProvider createProvider(
            String providerName,
            String providerCode,
            String environment,
            String merchantId) {

        return createProvider(
                providerName,
                providerCode,
                environment,
                merchantId,
                100
        );
    }

    /**
     * Registers a new payment provider with a routing priority.
     *
     * A lower priority number means a higher routing preference.
     * If no priority is supplied, the default priority is 100.
     */
    @Transactional
    public PaymentProvider createProvider(
            String providerName,
            String providerCode,
            String environment,
            String merchantId,
            Integer routingPriority) {

        // Validate required values.
        if (providerName == null || providerName.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Provider name is required."
            );
        }

        if (providerCode == null || providerCode.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Provider code is required."
            );
        }

        if (environment == null || environment.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Environment is required."
            );
        }

        if (routingPriority == null || routingPriority < 1) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Routing priority must be a positive integer."
            );
        }

        // Normalize values for consistent storage.
        String normalizedCode =
                providerCode.trim().toUpperCase(Locale.ROOT);

        String normalizedEnvironment =
                environment.trim().toUpperCase(Locale.ROOT);

        // Validate the environment.
        if (!normalizedEnvironment.equals("SANDBOX")
                && !normalizedEnvironment.equals("PRODUCTION")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Environment must be SANDBOX or PRODUCTION."
            );
        }

        // Reject duplicate provider codes.
        if (paymentProviderRepository.existsByProviderCode(
                normalizedCode)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A provider with this code already exists."
            );
        }

        // Create the provider entity.
        PaymentProvider provider = new PaymentProvider();

        provider.setProviderName(providerName.trim());
        provider.setProviderCode(normalizedCode);
        provider.setEnvironment(normalizedEnvironment);

        // Merchant ID remains optional.
        provider.setMerchantId(
                merchantId == null || merchantId.isBlank()
                        ? null
                        : merchantId.trim()
        );

        provider.setRoutingPriority(routingPriority);
        provider.setEnabled(true);

        return paymentProviderRepository.save(provider);
    }

    /**
     * Retrieves all configured payment providers.
     */
    public List<PaymentProvider> getAllProviders() {
        return paymentProviderRepository.findAll();
    }

    /**
     * Retrieves providers that are enabled for payment routing.
     */
    public List<PaymentProvider> getEnabledProviders() {
        return paymentProviderRepository.findByEnabled(true);
    }

    /**
     * Retrieves a provider by its database ID.
     */
    public PaymentProvider getProviderById(Long id) {

        if (id == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Provider ID is required."
            );
        }

        return paymentProviderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Payment provider not found."
                ));
    }

    /**
     * Enables or disables a configured provider.
     */
    @Transactional
    public PaymentProvider updateProviderStatus(
            Long id,
            boolean enabled) {

        PaymentProvider provider = getProviderById(id);

        provider.setEnabled(enabled);

        return paymentProviderRepository.save(provider);
    }

    /**
     * Updates the routing priority of an existing provider.
     *
     * Lower numbers indicate higher routing preference.
     */
    @Transactional
    public PaymentProvider updateRoutingPriority(
            Long id,
            Integer routingPriority) {

        if (routingPriority == null || routingPriority < 1) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Routing priority must be a positive integer."
            );
        }

        PaymentProvider provider = getProviderById(id);

        provider.setRoutingPriority(routingPriority);

        return paymentProviderRepository.save(provider);
    }

    /**
     * Retrieves providers by their configured environment.
     */
    public List<PaymentProvider> getProvidersByEnvironment(
            String environment) {

        if (environment == null || environment.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Environment is required."
            );
        }

        String normalizedEnvironment =
                environment.trim().toUpperCase(Locale.ROOT);

        if (!normalizedEnvironment.equals("SANDBOX")
                && !normalizedEnvironment.equals("PRODUCTION")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Environment must be SANDBOX or PRODUCTION."
            );
        }

        return paymentProviderRepository.findByEnvironment(
                normalizedEnvironment);
    }
}