package com.pratosh.orchestration.service;

import com.pratosh.orchestration.entity.PaymentMethod;
import com.pratosh.orchestration.entity.PaymentMethodConfiguration;
import com.pratosh.orchestration.entity.PaymentProvider;
import com.pratosh.orchestration.repository.PaymentMethodConfigurationRepository;
import com.pratosh.orchestration.repository.PaymentProviderRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

//Handles the business logic for configuring payment methods for payment providers such as Razorpay and PayU.
@Service
@Transactional(readOnly = true)
public class PaymentMethodConfigurationService {

    private final PaymentMethodConfigurationRepository configurationRepository;
    private final PaymentProviderRepository providerRepository;

    //Constructor injection provides the required repositories.

    public PaymentMethodConfigurationService(
            PaymentMethodConfigurationRepository configurationRepository,
            PaymentProviderRepository providerRepository) {

        this.configurationRepository = configurationRepository;
        this.providerRepository = providerRepository;
    }

    /**
      Adds a payment method to a payment provider.
      Example:
            Provider ID = 1
          Payment method = UPI
     */
    @Transactional
    public PaymentMethodConfiguration addPaymentMethod(
            Long providerId,
            PaymentMethod paymentMethod) {

        // Validate that a provider ID was supplied.
        if (providerId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Provider ID is required."
            );
        }

        // Validate that a payment method was supplied.
        if (paymentMethod == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Payment method is required."
            );
        }

        // Retrieve the provider or return HTTP 404.
        PaymentProvider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Payment provider not found."
                ));

        // Do not allow configuration for a disabled provider.
        if (!provider.isEnabled()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot configure payment methods for a disabled provider."
            );
        }

        // Prevent duplicate configurations for the same provider and payment method.

        boolean alreadyExists =
                configurationRepository
                        .existsByProvider_IdAndPaymentMethod(
                                providerId,
                                paymentMethod
                        );

        if (alreadyExists) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This payment method is already configured for the provider."
            );
        }

        // Create the new configuration.
        PaymentMethodConfiguration configuration =
                new PaymentMethodConfiguration();

        configuration.setProvider(provider);
        configuration.setPaymentMethod(paymentMethod);
        configuration.setEnabled(true);

        // Persist the configuration in MySQL.
        return configurationRepository.save(configuration);
    }


     //Retrieves every configured payment method for a provider, including disabled methods.

    public List<PaymentMethodConfiguration> getPaymentMethods(
            Long providerId) {

        // Verify that the requested provider exists.
        ensureProviderExists(providerId);

        return configurationRepository.findByProvider_Id(providerId);
    }


    //Retrieves only enabled payment methods for a provider.
    public List<PaymentMethodConfiguration> getEnabledPaymentMethods(
            Long providerId) {

        // Verify that the requested provider exists.
        PaymentProvider provider = ensureProviderExists(providerId);

        // A disabled provider must not expose enabled payment methods.
        if (!provider.isEnabled()) {
            return List.of();
        }

        return configurationRepository
                .findByProvider_IdAndEnabledTrue(providerId);
    }
// Enables or disables an existing payment method configuration.
    @Transactional
    public PaymentMethodConfiguration updatePaymentMethodStatus(
            Long configurationId,
            boolean enabled) {

        // Locate the configuration.
        PaymentMethodConfiguration configuration =
                configurationRepository.findById(configurationId)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Payment method configuration not found."
                        ));

        // Do not enable a method when its provider is disabled.
        if (enabled && !configuration.getProvider().isEnabled()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot enable a payment method for a disabled provider."
            );
        }

        // Update the status and save the changes.
        configuration.setEnabled(enabled);

        return configurationRepository.save(configuration);
    }


     //Retrieves a single payment method configuration by ID.

    public PaymentMethodConfiguration getPaymentMethodById(
            Long configurationId) {

        return configurationRepository.findById(configurationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Payment method configuration not found."
                ));
    }


     //Shared validation method for checking provider existence.
    private PaymentProvider ensureProviderExists(Long providerId) {

        if (providerId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Provider ID is required."
            );
        }

        return providerRepository.findById(providerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Payment provider not found."
                ));
    }
}