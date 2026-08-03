package com.ikano.onboarding.service;

import com.ikano.onboarding.domain.ApplicationStatus;
import com.ikano.onboarding.domain.Country;
import com.ikano.onboarding.domain.CustomerType;
import com.ikano.onboarding.domain.DecisionOutcome;
import com.ikano.onboarding.domain.IntegrationType;
import com.ikano.onboarding.entity.OnboardingApplicationEntity;
import com.ikano.onboarding.repository.IntegrationResultRepository;
import com.ikano.onboarding.service.dto.StepSubmissionOutcome;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
@DirtiesContext
class OnboardingServiceTest {

    @Autowired
    private OnboardingService onboardingService;

    @Autowired
    private IntegrationResultRepository integrationResultRepository;

    private Map<String, String> map(String... kv) {
        Map<String, String> m = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put(kv[i], kv[i + 1]);
        }
        return m;
    }

    @Test
    void swedenPrivateHappyPathIsApproved() {
        OnboardingApplicationEntity app = onboardingService.start(Country.SWEDEN, CustomerType.PRIVATE);

        assertSuccess(onboardingService.submitStep(app.getId(), "IDENTITY_VERIFICATION",
                map("fullName", "Anna Andersson", "idNumber", "19850101-1235")));

        assertSuccess(onboardingService.submitStep(app.getId(), "CONTACT_DETAILS",
                map("addressLine", "Storgatan 1", "postalCode", "123 45", "city", "Stockholm", "taxResidency", "SE")));

        assertSuccess(onboardingService.submitStep(app.getId(), "CONSENT_DECLARATIONS",
                map("consentTerms", "true")));

        assertSuccess(onboardingService.submitStep(app.getId(), "FINANCIAL_PROFILE",
                map("employmentStatus", "EMPLOYED", "monthlyIncome", "30000",
                        "monthlyDebtPayments", "2000", "housingCost", "8000")));

        assertSuccess(onboardingService.submitStep(app.getId(), "CREDIT_DECISION", Map.of()));

        StepSubmissionOutcome finalOutcome = onboardingService.submitStep(app.getId(), "REVIEW_SUBMIT",
                map("acceptTerms", "true"));
        assertTrue(finalOutcome.success());
        assertTrue(finalOutcome.applicationComplete());

        OnboardingApplicationEntity finished = onboardingService.get(app.getId());
        assertEquals(DecisionOutcome.APPROVED, finished.getFinalDecision());
        assertEquals(ApplicationStatus.APPROVED, finished.getStatus());
    }

    @Test
    void identityFailureBlocksProgressUntilCorrected() {
        OnboardingApplicationEntity app = onboardingService.start(Country.POLAND, CustomerType.PRIVATE);

        StepSubmissionOutcome failed = onboardingService.submitStep(app.getId(), "IDENTITY_VERIFICATION",
                map("fullName", "Jan Kowalski", "idNumber", "12345678900"));
        assertFalse(failed.success());
        assertFalse(failed.applicationComplete());
        assertEquals("IDENTITY_VERIFICATION", onboardingService.get(app.getId()).getCurrentStepKey());

        StepSubmissionOutcome corrected = onboardingService.submitStep(app.getId(), "IDENTITY_VERIFICATION",
                map("fullName", "Jan Kowalski", "idNumber", "12345678903"));
        assertSuccess(corrected);
        assertEquals("CONTACT_DETAILS", onboardingService.get(app.getId()).getCurrentStepKey());
    }

    @Test
    void possibleSanctionsHitRoutesToManualReview() {
        OnboardingApplicationEntity app = onboardingService.start(Country.SPAIN, CustomerType.PRIVATE);

        assertSuccess(onboardingService.submitStep(app.getId(), "IDENTITY_VERIFICATION",
                map("fullName", "Maria Garcia", "idNumber", "12345678Z")));
        assertSuccess(onboardingService.submitStep(app.getId(), "CONTACT_DETAILS",
                map("addressLine", "Calle Mayor 1", "province", "Madrid", "postalCode", "28001",
                        "city", "Madrid", "taxResidency", "ES")));
        assertSuccess(onboardingService.submitStep(app.getId(), "CONSENT_DECLARATIONS",
                map("consentTerms", "true", "simulateSanctionsHit", "true")));
        assertSuccess(onboardingService.submitStep(app.getId(), "FINANCIAL_PROFILE",
                map("employmentStatus", "EMPLOYED", "monthlyIncome", "3000",
                        "monthlyDebtPayments", "200", "housingCost", "800")));
        assertSuccess(onboardingService.submitStep(app.getId(), "CREDIT_DECISION", Map.of()));

        StepSubmissionOutcome finalOutcome = onboardingService.submitStep(app.getId(), "REVIEW_SUBMIT",
                map("acceptTerms", "true"));
        assertTrue(finalOutcome.applicationComplete());

        OnboardingApplicationEntity finished = onboardingService.get(app.getId());
        assertEquals(DecisionOutcome.MANUAL_REVIEW, finished.getFinalDecision());
    }

    @Test
    void resubmittingACompletedStepWithUnchangedDataDoesNotRerunTheIntegration() {
        OnboardingApplicationEntity app = onboardingService.start(Country.SWEDEN, CustomerType.PRIVATE);
        Map<String, String> identity = map("fullName", "Anna Andersson", "idNumber", "19850101-1235");

        assertSuccess(onboardingService.submitStep(app.getId(), "IDENTITY_VERIFICATION", identity));
        long callsAfterFirst = countIdentityChecks(app.getId());
        assertEquals(1, callsAfterFirst);

        // Simulate a resumed session resubmitting the same step (e.g. browser back + submit again).
        onboardingService.submitStep(app.getId(), "IDENTITY_VERIFICATION", identity);
        long callsAfterResubmit = countIdentityChecks(app.getId());

        assertEquals(callsAfterFirst, callsAfterResubmit, "Unchanged resubmission should not call the mock identity client again");
    }

    @Test
    void resumeTokenResolvesBackToTheSameApplication() {
        OnboardingApplicationEntity app = onboardingService.start(Country.SWEDEN, CustomerType.PRIVATE);

        var resumed = onboardingService.resolveResumeToken(app.getResumeToken());
        assertTrue(resumed.isPresent());
        assertEquals(app.getId(), resumed.get().getId());
        assertEquals(ApplicationStatus.IN_PROGRESS, resumed.get().getStatus());
    }

    @Test
    void unknownResumeTokenResolvesToEmpty() {
        assertTrue(onboardingService.resolveResumeToken(UUID.randomUUID().toString()).isEmpty());
    }

    private long countIdentityChecks(UUID applicationId) {
        return integrationResultRepository.findByApplicationIdOrderByCheckedAtAsc(applicationId).stream()
                .filter(r -> r.getIntegrationType() == IntegrationType.IDENTITY_KYC)
                .count();
    }

    private void assertSuccess(StepSubmissionOutcome outcome) {
        assertTrue(outcome.success(), () -> "Expected success but got errors=" + outcome.fieldErrors()
                + " failure=" + outcome.integrationFailureMessage());
    }
}
