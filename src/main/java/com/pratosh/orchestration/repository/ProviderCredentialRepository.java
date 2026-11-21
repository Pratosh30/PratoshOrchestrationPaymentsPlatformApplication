package com.pratosh.orchestration.repository;

import com.pratosh.orchestration.entity.ProviderCredential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/*
  Repository for accessing provider credential records.
 Spring Data JPA generates the implementation automatically.
 IMPORTANT:
  This repository returns entity objects containing encrypted credentials. Never return these entities directly from a REST API.
 */
public interface ProviderCredentialRepository
        extends JpaRepository<ProviderCredential, Long> {

    /*
    Finds all credential records belonging to a provider.
     Example:
      Retrieve the credential configurations associated with Razorpay.
     */
    List<ProviderCredential> findByProvider_Id(Long providerId);

    /*
    Finds credentials for a provider in a specific environment.
     Example:
      Retrieve Razorpay's SANDBOX credentials.
     */
    List<ProviderCredential> findByProvider_IdAndEnvironment(
            Long providerId,
            String environment
    );


     //Finds a credential configuration using its reference ID.

    Optional<ProviderCredential> findByReferenceId(String referenceId);

    // Checks whether a reference ID is already registered for a particular provider and environment.

    boolean existsByProvider_IdAndEnvironmentAndReferenceId(
            Long providerId,
            String environment,
            String referenceId
    );


     // Retrieves only enabled credentials for a provider.

    List<ProviderCredential> findByProvider_IdAndEnabledTrue(
            Long providerId
    );

    // Retrieves enabled credentials for a provider and environment.

    List<ProviderCredential> findByProvider_IdAndEnvironmentAndEnabledTrue(
            Long providerId,
            String environment
    );
}