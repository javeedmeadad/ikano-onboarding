package com.ikano.onboarding.integration;

import com.ikano.onboarding.domain.IntegrationType;
import org.springframework.stereotype.Component;

/**
 * Mock PEP/sanctions screening. A name containing "sanctioned" (case-insensitive) is a
 * deterministic confirmed hit for demo/testing; otherwise the explicit "simulate a hit" checkbox
 * on the consent/UBO step drives a manual-review outcome.
 */
@Component
public class PepSanctionsMockClient implements IntegrationClient {

    @Override
    public IntegrationType type() {
        return IntegrationType.PEP_SANCTIONS;
    }

    @Override
    public IntegrationResponse call(IntegrationRequest request) {
        String name = request.field("uboName") != null ? request.field("uboName") : request.field("fullName");
        if (name != null && name.toLowerCase().contains("sanctioned")) {
            return IntegrationResponse.fail("confirmed_hit", "Name matches a confirmed entry on the sanctions list.");
        }

        boolean simulateHit = "true".equalsIgnoreCase(request.field("simulateSanctionsHit"))
                || "on".equalsIgnoreCase(request.field("simulateSanctionsHit"));
        if (simulateHit) {
            return IntegrationResponse.manualReview("possible_hit", "Possible sanctions/PEP match requires manual review.");
        }

        return IntegrationResponse.success("no_hit", "No sanctions or PEP match found.");
    }
}
