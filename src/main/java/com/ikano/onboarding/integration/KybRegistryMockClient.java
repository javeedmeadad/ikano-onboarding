package com.ikano.onboarding.integration;

import com.ikano.onboarding.domain.IntegrationType;
import org.springframework.stereotype.Component;

/**
 * Mock Bolagsverket / Registro Mercantil / CEIDG-KRS style company registry lookup. Same
 * last-digit determinism as {@link IdentityMockClient}, applied to the company identifier.
 */
@Component
public class KybRegistryMockClient implements IntegrationClient {

    @Override
    public IntegrationType type() {
        return IntegrationType.KYB_REGISTRY;
    }

    @Override
    public IntegrationResponse call(IntegrationRequest request) {
        char lastDigit = MockRules.lastDigit(request.field("companyId"));

        return switch (lastDigit) {
            case '0' -> IntegrationResponse.fail("dissolved", "Company registry shows this entity as dissolved.");
            case '9' -> IntegrationResponse.manualReview("unknown_representative", "Registry could not confirm the representative automatically.");
            default -> IntegrationResponse.success("active_company", "Company is active and in good standing.");
        };
    }
}
