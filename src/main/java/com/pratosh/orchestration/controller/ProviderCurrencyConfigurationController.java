package com.pratosh.orchestration.controller;

import com.pratosh.orchestration.entity.ProviderCurrencyConfiguration;
import com.pratosh.orchestration.service.ProviderCurrencyConfigurationService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
public class ProviderCurrencyConfigurationController {

    private final ProviderCurrencyConfigurationService currencyService;

    public ProviderCurrencyConfigurationController(
            ProviderCurrencyConfigurationService currencyService) {
        this.currencyService = currencyService;
    }

    @PostMapping("/providers/{providerId}/currencies")
    public ResponseEntity<Map<String, Object>> addCurrency(
            @PathVariable Long providerId,
            @RequestBody Map<String, String> request) {

        String currencyCode = request.get("currencyCode");

        ProviderCurrencyConfiguration configuration =
                currencyService.addCurrency(providerId, currencyCode);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(configuration));
    }

    @GetMapping("/providers/{providerId}/currencies")
    public ResponseEntity<List<Map<String, Object>>> getCurrencies(
            @PathVariable Long providerId) {

        List<Map<String, Object>> response =
                currencyService.getCurrenciesByProvider(providerId)
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/providers/{providerId}/currencies/enabled")
    public ResponseEntity<List<Map<String, Object>>> getEnabledCurrencies(
            @PathVariable Long providerId) {

        List<Map<String, Object>> response =
                currencyService.getEnabledCurrenciesByProvider(providerId)
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/currencies/{configurationId}/status")
    public ResponseEntity<Map<String, Object>> updateCurrencyStatus(
            @PathVariable Long configurationId,
            @RequestParam boolean enabled) {

        ProviderCurrencyConfiguration configuration =
                currencyService.updateCurrencyStatus(
                        configurationId, enabled);

        return ResponseEntity.ok(toResponse(configuration));
    }

    private Map<String, Object> toResponse(
            ProviderCurrencyConfiguration configuration) {

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("id", configuration.getId());
        response.put("providerId", configuration.getProvider().getId());
        response.put("providerName",
                configuration.getProvider().getProviderName());
        response.put("currencyCode", configuration.getCurrencyCode());
        response.put("enabled", configuration.isEnabled());
        response.put("createdAt", configuration.getCreatedAt());
        response.put("updatedAt", configuration.getUpdatedAt());

        return response;
    }
}