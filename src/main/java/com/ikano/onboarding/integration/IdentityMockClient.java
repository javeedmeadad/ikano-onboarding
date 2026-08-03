package com.ikano.onboarding.integration;

import com.ikano.onboarding.domain.IntegrationType;
import org.springframework.stereotype.Component;

/**
 * Mock BankID / Clave-DNIe / eID style identity check, also reused for the business-flow
 * representative check. Deterministic rule (documented in README "demo values" table): the last
 * digit of the id-like field decides the outcome so QA can reliably trigger every branch.
 */
@Component
public class IdentityMockClient implements IntegrationClient {

    @Override
    public IntegrationType type() {
        return IntegrationType.IDENTITY_KYC;
    }

    @Override
    public IntegrationResponse call(IntegrationRequest request) {
        String idValue = request.field("idNumber") != null ? request.field("idNumber") : request.field("repIdNumber");
        char lastDigit = MockRules.lastDigit(idValue);

        return switch (lastDigit) {
            case '0' -> IntegrationResponse.fail("expired_id", "Identity document appears expired.");
            case '9' -> IntegrationResponse.manualReview("manual_review", "Identity check could not be automatically confirmed.");
            default -> IntegrationResponse.success("verified", "Identity verified.");
        };
    }
}
