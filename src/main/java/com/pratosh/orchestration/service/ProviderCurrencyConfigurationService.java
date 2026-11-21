package com.pratosh.orchestration.service;

import com.pratosh.orchestration.entity.PaymentProvider;
import com.pratosh.orchestration.entity.ProviderCurrencyConfiguration;
import com.pratosh.orchestration.repository.PaymentProviderRepository;
import com.pratosh.orchestration.repository.ProviderCurrencyConfigurationRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

@Service
public class ProviderCurrencyConfigurationService {

    private final ProviderCurrencyConfigurationRepository currencyRepository;
    private final PaymentProviderRepository providerRepository;

    public ProviderCurrencyConfigurationService(
            ProviderCurrencyConfigurationRepository currencyRepository,
            PaymentProviderRepository providerRepository) {
        this.currencyRepository = currencyRepository;
        this.providerRepository = providerRepository;
    }

    @Transactional
    public ProviderCurrencyConfiguration addCurrency(
            Long providerId,
            String currencyCode) {

        if (providerId == null) {
            throw new IllegalArgumentException("Provider ID is required");
        }

        if (currencyCode == null
                || !currencyCode.trim().matches("(?i)[A-Z]{3}")) {
            throw new IllegalArgumentException(
                    "Currency must be a valid three-letter code, such as INR or USD");
        }

        String normalizedCurrency =
                currencyCode.trim().toUpperCase(Locale.ROOT);

        PaymentProvider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Payment provider not found: " + providerId));

        boolean alreadyExists =
                currencyRepository
                        .findByProvider_IdAndCurrencyCode(
                                providerId, normalizedCurrency)
                        .isPresent();

        if (alreadyExists) {
            throw new IllegalArgumentException(
                    "Currency " + normalizedCurrency
                            + " is already configured for provider "
                            + providerId);
        }

        ProviderCurrencyConfiguration configuration =
                new ProviderCurrencyConfiguration();

        configuration.setProvider(provider);
        configuration.setCurrencyCode(normalizedCurrency);
        configuration.setEnabled(true);

        return currencyRepository.save(configuration);
    }

    @Transactional(readOnly = true)
    public List<ProviderCurrencyConfiguration> getCurrenciesByProvider(
            Long providerId) {

        if (providerId == null) {
            throw new IllegalArgumentException("Provider ID is required");
        }

        if (!providerRepository.existsById(providerId)) {
            throw new NoSuchElementException(
                    "Payment provider not found: " + providerId);
        }

        return currencyRepository.findByProvider_Id(providerId);
    }

    @Transactional(readOnly = true)
    public List<ProviderCurrencyConfiguration> getEnabledCurrenciesByProvider(
            Long providerId) {

        if (providerId == null) {
            throw new IllegalArgumentException("Provider ID is required");
        }

        if (!providerRepository.existsById(providerId)) {
            throw new NoSuchElementException(
                    "Payment provider not found: " + providerId);
        }

        return currencyRepository
                .findByProvider_IdAndEnabledTrue(providerId);
    }

    @Transactional
    public ProviderCurrencyConfiguration updateCurrencyStatus(
            Long configurationId,
            boolean enabled) {

        if (configurationId == null) {
            throw new IllegalArgumentException(
                    "Currency configuration ID is required");
        }

        ProviderCurrencyConfiguration configuration =
                currencyRepository.findById(configurationId)
                        .orElseThrow(() -> new NoSuchElementException(
                                "Currency configuration not found: "
                                        + configurationId));

        configuration.setEnabled(enabled);

        return currencyRepository.save(configuration);
    }
}