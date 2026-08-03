package com.ikano.onboarding.integration;

import com.ikano.onboarding.domain.Country;
import com.ikano.onboarding.domain.CustomerType;
import com.ikano.onboarding.domain.IntegrationOutcome;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CreditAffordabilityMockClientTest {

    private final CreditAffordabilityMockClient client = new CreditAffordabilityMockClient();

    @Test
    void negativeDisposableIncomeFails() {
        var request = new IntegrationRequest("r1", Country.SWEDEN, CustomerType.PRIVATE, Map.of(
                "monthlyIncome", "10000", "monthlyDebtPayments", "5000", "housingCost", "8000"), 1);
        assertEquals(IntegrationOutcome.FAIL, client.call(request).outcome());
    }

    @Test
    void thinMarginNeedsManualReview() {
        var request = new IntegrationRequest("r2", Country.SWEDEN, CustomerType.PRIVATE, Map.of(
                "monthlyIncome", "10000", "monthlyDebtPayments", "2000", "housingCost", "7600"), 1);
        assertEquals(IntegrationOutcome.MANUAL_REVIEW, client.call(request).outcome());
    }

    @Test
    void healthyMarginSucceeds() {
        var request = new IntegrationRequest("r3", Country.SWEDEN, CustomerType.PRIVATE, Map.of(
                "monthlyIncome", "30000", "monthlyDebtPayments", "2000", "housingCost", "8000"), 1);
        assertEquals(IntegrationOutcome.SUCCESS, client.call(request).outcome());
    }

    @Test
    void businessWithNoTurnoverFails() {
        var request = new IntegrationRequest("r4", Country.SWEDEN, CustomerType.BUSINESS,
                Map.of("annualTurnover", "0"), 1);
        assertEquals(IntegrationOutcome.FAIL, client.call(request).outcome());
    }

    @Test
    void businessBelowThresholdNeedsManualReview() {
        var request = new IntegrationRequest("r5", Country.SWEDEN, CustomerType.BUSINESS,
                Map.of("annualTurnover", "20000"), 1);
        assertEquals(IntegrationOutcome.MANUAL_REVIEW, client.call(request).outcome());
    }

    @Test
    void businessAboveThresholdSucceeds() {
        var request = new IntegrationRequest("r6", Country.SWEDEN, CustomerType.BUSINESS,
                Map.of("annualTurnover", "500000"), 1);
        assertEquals(IntegrationOutcome.SUCCESS, client.call(request).outcome());
    }
}
