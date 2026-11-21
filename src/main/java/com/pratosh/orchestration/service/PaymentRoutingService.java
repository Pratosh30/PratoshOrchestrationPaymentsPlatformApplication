package com.pratosh.orchestration.service;

import com.pratosh.orchestration.entity.PaymentMethod;
import com.pratosh.orchestration.entity.PaymentMethodConfiguration;
import com.pratosh.orchestration.entity.PaymentProvider;
import com.pratosh.orchestration.entity.ProviderCredential;

import com.pratosh.orchestration.repository.PaymentMethodConfigurationRepository;
import com.pratosh.orchestration.repository.PaymentProviderRepository;
import com.pratosh.orchestration.repository.ProviderCredentialRepository;
import com.pratosh.orchestration.repository.ProviderCurrencyConfigurationRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class PaymentRoutingService {

    private final PaymentProviderRepository providerRepository;
    private final PaymentMethodConfigurationRepository methodRepository;
    private final ProviderCredentialRepository credentialRepository;
    private final ProviderCurrencyConfigurationRepository currencyRepository;

    public PaymentRoutingService(
            PaymentProviderRepository providerRepository,
            PaymentMethodConfigurationRepository methodRepository,
            ProviderCredentialRepository credentialRepository,
            ProviderCurrencyConfigurationRepository currencyRepository) {

        this.providerRepository = providerRepository;
        this.methodRepository = methodRepository;
        this.credentialRepository = credentialRepository;
        this.currencyRepository = currencyRepository;
    }

    /**
     * Selects the highest-priority eligible payment provider.
     *
     * Eligibility requirements:
     * 1. Provider is enabled.
     * 2. Requested payment method is enabled for the provider.
     * 3. Requested currency is enabled for the provider.
     * 4. Provider has enabled credentials for the requested environment.
     *
     * Lower routing priority numbers are preferred.
     */
    @Transactional(readOnly = true)
    public PaymentProvider selectProvider(
            PaymentMethod paymentMethod,
            String currency,
            String environment) {

        // Validate payment method.
        if (paymentMethod == null) {
            throw new IllegalArgumentException(
                    "Payment method is required.");
        }

        // Validate currency format.
        if (currency == null
                || !currency.trim().matches("(?i)[A-Z]{3}")) {

            throw new IllegalArgumentException(
                    "Currency must be a three-letter code.");
        }

        // Validate environment.
        if (environment == null || environment.isBlank()) {
            throw new IllegalArgumentException(
                    "Payment environment is required.");
        }

        String normalizedCurrency =
                currency.trim().toUpperCase(Locale.ROOT);

        String normalizedEnvironment =
                environment.trim().toUpperCase(Locale.ROOT);

        if (!normalizedEnvironment.equals("SANDBOX")
                && !normalizedEnvironment.equals("PRODUCTION")) {

            throw new IllegalArgumentException(
                    "Environment must be SANDBOX or PRODUCTION.");
        }

        // Retrieve providers that are enabled.
        List<PaymentProvider> enabledProviders =
                providerRepository.findByEnabled(true);

        // Filter providers based on eligibility and select the
        // provider with the lowest routing priority number.
        return enabledProviders.stream()

                .filter(provider ->
                        supportsPaymentMethod(
                                provider.getId(),
                                paymentMethod))

                .filter(provider ->
                        supportsCurrency(
                                provider.getId(),
                                normalizedCurrency))

                .filter(provider ->
                        hasEnabledCredential(
                                provider.getId(),
                                normalizedEnvironment))

                .sorted(
                        Comparator.comparing(
                                        PaymentProvider::getRoutingPriority,
                                        Comparator.nullsLast(
                                                Comparator.naturalOrder()))
                                .thenComparing(
                                        PaymentProvider::getId,
                                        Comparator.nullsLast(
                                                Comparator.naturalOrder()))
                )

                .findFirst()

                .orElseThrow(() -> new IllegalStateException(
                        "No eligible payment provider found for payment method "
                                + paymentMethod
                                + ", currency "
                                + normalizedCurrency
                                + ", and environment "
                                + normalizedEnvironment));
    }

    /**
     * Checks whether the provider supports the requested payment method.
     */
    private boolean supportsPaymentMethod(
            Long providerId,
            PaymentMethod paymentMethod) {

        List<PaymentMethodConfiguration> configurations =
                methodRepository.findByProvider_IdAndEnabledTrue(
                        providerId);

        return configurations.stream()
                .anyMatch(configuration ->
                        configuration.getPaymentMethod() == paymentMethod);
    }

    /**
     * Checks whether the provider supports the requested currency.
     */
    private boolean supportsCurrency(
            Long providerId,
            String currency) {

        return currencyRepository
                .existsByProvider_IdAndCurrencyCodeAndEnabledTrue(
                        providerId,
                        currency);
    }

    /**
     * Checks whether the provider has enabled credentials
     * for the requested environment.
     */
    private boolean hasEnabledCredential(
            Long providerId,
            String environment) {

        List<ProviderCredential> credentials =
                credentialRepository
                        .findByProvider_IdAndEnvironment(
                                providerId,
                                environment);

        return credentials.stream()
                .anyMatch(ProviderCredential::isEnabled);
    }

    @Transactional(readOnly = true)
    public PaymentProvider selectAlternativeProvider(
            PaymentMethod paymentMethod,
            String currency,
            String environment,
            Long excludedProviderId) {

        if (excludedProviderId == null) {
            throw new IllegalArgumentException(
                    "The provider to exclude is required."
            );
        }

        PaymentProvider alternativeProvider =
                selectProvider(paymentMethod, currency, environment);

        if (!excludedProviderId.equals(alternativeProvider.getId())) {
            return alternativeProvider;
        }

        List<PaymentProvider> enabledProviders =
                providerRepository.findByEnabled(true);

        String normalizedCurrency =
                currency.trim().toUpperCase(Locale.ROOT);

        String normalizedEnvironment =
                environment.trim().toUpperCase(Locale.ROOT);

        return enabledProviders.stream()
                .filter(provider ->
                        !excludedProviderId.equals(provider.getId()))
                .filter(provider ->
                        supportsPaymentMethod(
                                provider.getId(),
                                paymentMethod))
                .filter(provider ->
                        supportsCurrency(
                                provider.getId(),
                                normalizedCurrency))
                .filter(provider ->
                        hasEnabledCredential(
                                provider.getId(),
                                normalizedEnvironment))
                .sorted(Comparator.comparing(
                                PaymentProvider::getRoutingPriority,
                                Comparator.nullsLast(
                                        Comparator.naturalOrder()))
                        .thenComparing(
                                PaymentProvider::getId,
                                Comparator.nullsLast(
                                        Comparator.naturalOrder())))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No eligible alternative payment provider found."
                ));
    }
}