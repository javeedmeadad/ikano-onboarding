package com.ikano.onboarding.integration;

import com.ikano.onboarding.domain.Country;
import com.ikano.onboarding.domain.CustomerType;
import com.ikano.onboarding.domain.IntegrationOutcome;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IdentityMockClientTest {

    private final IdentityMockClient client = new IdentityMockClient();

    private IntegrationRequest requestWithId(String idNumber) {
        return new IntegrationRequest("req-1", Country.SWEDEN, CustomerType.PRIVATE, Map.of("idNumber", idNumber), 1);
    }

    @Test
    void idEndingInZeroFails() {
        assertEquals(IntegrationOutcome.FAIL, client.call(requestWithId("19850101-1230")).outcome());
    }

    @Test
    void idEndingInNineNeedsManualReview() {
        assertEquals(IntegrationOutcome.MANUAL_REVIEW, client.call(requestWithId("19850101-1239")).outcome());
    }

    @Test
    void anyOtherIdSucceeds() {
        assertEquals(IntegrationOutcome.SUCCESS, client.call(requestWithId("19850101-1235")).outcome());
    }

    @Test
    void fallsBackToRepresentativeIdForBusinessFlow() {
        var request = new IntegrationRequest("req-2", Country.SWEDEN, CustomerType.BUSINESS,
                Map.of("repIdNumber", "19850101-1230"), 1);
        assertEquals(IntegrationOutcome.FAIL, client.call(request).outcome());
    }
}
