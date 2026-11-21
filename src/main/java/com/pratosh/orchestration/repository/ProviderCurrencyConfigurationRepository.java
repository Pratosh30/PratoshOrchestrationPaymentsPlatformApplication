package com.pratosh.orchestration.repository;

import com.pratosh.orchestration.entity.ProviderCurrencyConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProviderCurrencyConfigurationRepository
        extends JpaRepository<ProviderCurrencyConfiguration, Long> {

    // Find all currency configurations for a provider.
    List<ProviderCurrencyConfiguration> findByProvider_Id(Long providerId);

    // Find only enabled currency configurations for a provider.
    List<ProviderCurrencyConfiguration> findByProvider_IdAndEnabledTrue(Long providerId);

    // Find a specific currency configuration for a provider.
    Optional<ProviderCurrencyConfiguration> findByProvider_IdAndCurrencyCode(
            Long providerId,
            String currencyCode
    );

    // Check whether a currency is enabled for a provider.
    boolean existsByProvider_IdAndCurrencyCodeAndEnabledTrue(
            Long providerId,
            String currencyCode
    );

    // Find an enabled configuration for a particular provider and currency.
    Optional<ProviderCurrencyConfiguration> findByProvider_IdAndCurrencyCodeAndEnabledTrue(
            Long providerId,
            String currencyCode
    );
}