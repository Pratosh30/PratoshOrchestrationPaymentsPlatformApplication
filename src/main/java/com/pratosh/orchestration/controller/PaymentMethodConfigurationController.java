package com.pratosh.orchestration.controller;

import com.pratosh.orchestration.dto.PaymentMethodConfigurationRequest;
import com.pratosh.orchestration.dto.PaymentMethodConfigurationResponse;
import com.pratosh.orchestration.entity.PaymentMethodConfiguration;
import com.pratosh.orchestration.service.PaymentMethodConfigurationService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

  //REST controller for managing payment method configurations.
  //All endpoints are protected by the administrator security rules configured in SecurityConfig.java.

@RestController
@RequestMapping("/api/v1/admin")
public class PaymentMethodConfigurationController {

    private final PaymentMethodConfigurationService configurationService;

    /**
     * Constructor injection for the service layer.
     */
    public PaymentMethodConfigurationController(
            PaymentMethodConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    /*
     Adds a payment method to a payment provider.
      Example:
         POST /api/v1/admin/providers/1/payment-methods
     */
    @PostMapping("/providers/{providerId}/payment-methods")
    public ResponseEntity<PaymentMethodConfigurationResponse> addPaymentMethod(
            @PathVariable Long providerId,
            @Valid @RequestBody PaymentMethodConfigurationRequest request) {

        // Execute business validation and save the configuration.
        PaymentMethodConfiguration configuration =
                configurationService.addPaymentMethod(
                        providerId,
                        request.getPaymentMethod()
                );

        // Convert the entity into a controlled API response.
        PaymentMethodConfigurationResponse response =
                PaymentMethodConfigurationResponse.fromEntity(configuration);

        // Return HTTP 201 because a new resource was created.
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /*
    Retrieves all payment method configurations for a provider, including configurations that are disabled.
     Example:
       GET /api/v1/admin/providers/1/payment-methods
     */
    @GetMapping("/providers/{providerId}/payment-methods")
    public ResponseEntity<List<PaymentMethodConfigurationResponse>>
    getPaymentMethods(@PathVariable Long providerId) {

        List<PaymentMethodConfigurationResponse> responses =
                configurationService.getPaymentMethods(providerId)
                        .stream()
                        .map(PaymentMethodConfigurationResponse::fromEntity)
                        .toList();

        return ResponseEntity.ok(responses);
    }

    /*
    Retrieves enabled payment methods for a provider.
      Example:
      GET /api/v1/admin/providers/1/payment-methods/enabled
     */
    @GetMapping("/providers/{providerId}/payment-methods/enabled")
    public ResponseEntity<List<PaymentMethodConfigurationResponse>>
    getEnabledPaymentMethods(@PathVariable Long providerId) {

        List<PaymentMethodConfigurationResponse> responses =
                configurationService.getEnabledPaymentMethods(providerId)
                        .stream()
                        .map(PaymentMethodConfigurationResponse::fromEntity)
                        .toList();

        return ResponseEntity.ok(responses);
    }

    /*
    Retrieves one payment method configuration by its ID.
     Example:
     GET /api/v1/admin/payment-methods/1
     */
    @GetMapping("/payment-methods/{configurationId}")
    public ResponseEntity<PaymentMethodConfigurationResponse>
    getPaymentMethodById(@PathVariable Long configurationId) {

        PaymentMethodConfiguration configuration =
                configurationService.getPaymentMethodById(configurationId);

        return ResponseEntity.ok(
                PaymentMethodConfigurationResponse.fromEntity(configuration)
        );
    }

    /*
    Enables or disables an existing payment method configuration.
     Example:
     PATCH /api/v1/admin/payment-methods/1/status?enabled=false
     */
    @PatchMapping("/payment-methods/{configurationId}/status")
    public ResponseEntity<PaymentMethodConfigurationResponse>
    updatePaymentMethodStatus(
            @PathVariable Long configurationId,
            @RequestParam boolean enabled) {

        PaymentMethodConfiguration configuration =
                configurationService.updatePaymentMethodStatus(
                        configurationId,
                        enabled
                );

        return ResponseEntity.ok(
                PaymentMethodConfigurationResponse.fromEntity(configuration)
        );
    }
}