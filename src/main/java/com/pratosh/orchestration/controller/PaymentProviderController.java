package com.pratosh.orchestration.controller;

import com.pratosh.orchestration.dto.PaymentProviderRequest;
import com.pratosh.orchestration.dto.PaymentProviderResponse;
import com.pratosh.orchestration.entity.PaymentProvider;
import com.pratosh.orchestration.service.PaymentProviderService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/providers")
public class PaymentProviderController {

    private final PaymentProviderService paymentProviderService;

    public PaymentProviderController(
            PaymentProviderService paymentProviderService) {
        this.paymentProviderService = paymentProviderService;
    }

    /**
     * Create a new payment provider.
     * POST /api/v1/admin/providers
     */
    @PostMapping
    public ResponseEntity<PaymentProviderResponse> createProvider(
            @Valid @RequestBody PaymentProviderRequest request) {

        PaymentProvider provider = paymentProviderService.createProvider(
                request.getProviderName(),
                request.getProviderCode(),
                request.getEnvironment(),
                request.getMerchantId(),
                request.getRoutingPriority()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(PaymentProviderResponse.fromEntity(provider));
    }

    /**
     * Retrieve all payment providers.
     * GET /api/v1/admin/providers
     */
    @GetMapping
    public ResponseEntity<List<PaymentProviderResponse>> getAllProviders() {

        List<PaymentProviderResponse> responses =
                paymentProviderService.getAllProviders()
                        .stream()
                        .map(PaymentProviderResponse::fromEntity)
                        .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    /**
     * Retrieve all enabled payment providers.
     * GET /api/v1/admin/providers/enabled
     */
    @GetMapping("/enabled")
    public ResponseEntity<List<PaymentProviderResponse>> getEnabledProviders() {

        List<PaymentProviderResponse> responses =
                paymentProviderService.getEnabledProviders()
                        .stream()
                        .map(PaymentProviderResponse::fromEntity)
                        .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    /**
     * Retrieve a payment provider by ID.
     * GET /api/v1/admin/providers/1
     */
    @GetMapping("/{id}")
    public ResponseEntity<PaymentProviderResponse> getProviderById(
            @PathVariable Long id) {

        PaymentProvider provider =
                paymentProviderService.getProviderById(id);

        return ResponseEntity.ok(
                PaymentProviderResponse.fromEntity(provider));
    }

    /**
     * Enable or disable a payment provider.
     * PATCH /api/v1/admin/providers/1/status?enabled=false
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<PaymentProviderResponse> updateProviderStatus(
            @PathVariable Long id,
            @RequestParam boolean enabled) {

        PaymentProvider provider =
                paymentProviderService.updateProviderStatus(id, enabled);

        return ResponseEntity.ok(
                PaymentProviderResponse.fromEntity(provider));
    }

    /**
     * Retrieve providers by environment.
     * GET /api/v1/admin/providers/environment/SANDBOX
     */
    @GetMapping("/environment/{environment}")
    public ResponseEntity<List<PaymentProviderResponse>>
    getProvidersByEnvironment(@PathVariable String environment) {

        List<PaymentProviderResponse> responses =
                paymentProviderService.getProvidersByEnvironment(environment)
                        .stream()
                        .map(PaymentProviderResponse::fromEntity)
                        .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    /**
     * Update a provider's routing priority.
     * PATCH /api/v1/admin/providers/1/priority
     *
     * Request body:
     * {
     *   "routingPriority": 10
     * }
     */
    @PatchMapping("/{id}/priority")
    public ResponseEntity<PaymentProviderResponse> updateRoutingPriority(
            @PathVariable Long id,
            @RequestBody Map<String, Integer> request) {

        Integer routingPriority = request.get("routingPriority");

        PaymentProvider provider =
                paymentProviderService.updateRoutingPriority(
                        id, routingPriority);

        return ResponseEntity.ok(
                PaymentProviderResponse.fromEntity(provider));
    }
}