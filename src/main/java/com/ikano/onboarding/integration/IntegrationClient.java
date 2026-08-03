package com.ikano.onboarding.integration;

import com.ikano.onboarding.domain.IntegrationType;

/**
 * Boundary for one external service. Every implementation here is a deterministic mock — no
 * network calls — but the interface is what a real client (e.g. calling a bureau's REST API)
 * would implement, so swapping a mock for the real thing later touches one class, not callers.
 */
public interface IntegrationClient {

    IntegrationType type();

    IntegrationResponse call(IntegrationRequest request);
}
