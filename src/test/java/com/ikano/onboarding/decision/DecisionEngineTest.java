package com.ikano.onboarding.decision;

import com.ikano.onboarding.domain.DecisionOutcome;
import com.ikano.onboarding.domain.IntegrationOutcome;
import com.ikano.onboarding.domain.IntegrationType;
import com.ikano.onboarding.entity.IntegrationResultEntity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DecisionEngineTest {

    private final DecisionEngine engine = new DecisionEngine();

    private IntegrationResultEntity result(IntegrationType type, IntegrationOutcome outcome, String detail) {
        IntegrationResultEntity e = new IntegrationResultEntity();
        e.setIntegrationType(type);
        e.setOutcome(outcome);
        e.setDetailCode(detail);
        return e;
    }

    @Test
    void allSuccessApproves() {
        var results = List.of(
                result(IntegrationType.IDENTITY_KYC, IntegrationOutcome.SUCCESS, "verified"),
                result(IntegrationType.PEP_SANCTIONS, IntegrationOutcome.SUCCESS, "no_hit"),
                result(IntegrationType.CREDIT_AFFORDABILITY, IntegrationOutcome.SUCCESS, "affordability_confirmed")
        );

        assertEquals(DecisionOutcome.APPROVED, engine.decide(results).outcome());
    }

    @Test
    void oneManualReviewRefersWholeApplication() {
        var results = List.of(
                result(IntegrationType.IDENTITY_KYC, IntegrationOutcome.SUCCESS, "verified"),
                result(IntegrationType.PEP_SANCTIONS, IntegrationOutcome.MANUAL_REVIEW, "possible_hit"),
                result(IntegrationType.CREDIT_AFFORDABILITY, IntegrationOutcome.SUCCESS, "affordability_confirmed")
        );

        assertEquals(DecisionOutcome.MANUAL_REVIEW, engine.decide(results).outcome());
    }

    @Test
    void oneFailureRejectsEvenWithManualReviewPresent() {
        var results = List.of(
                result(IntegrationType.IDENTITY_KYC, IntegrationOutcome.FAIL, "expired_id"),
                result(IntegrationType.PEP_SANCTIONS, IntegrationOutcome.MANUAL_REVIEW, "possible_hit"),
                result(IntegrationType.CREDIT_AFFORDABILITY, IntegrationOutcome.SUCCESS, "affordability_confirmed")
        );

        var decision = engine.decide(results);
        assertEquals(DecisionOutcome.REJECTED, decision.outcome());
        assertEquals(true, decision.reason().contains("IDENTITY_KYC"));
    }

    @Test
    void noChecksDefaultsToApproved() {
        assertEquals(DecisionOutcome.APPROVED, engine.decide(List.of()).outcome());
    }
}
