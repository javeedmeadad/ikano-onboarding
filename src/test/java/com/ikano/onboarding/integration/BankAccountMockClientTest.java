package com.ikano.onboarding.integration;

import com.ikano.onboarding.domain.Country;
import com.ikano.onboarding.domain.CustomerType;
import com.ikano.onboarding.domain.IntegrationOutcome;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BankAccountMockClientTest {

    private final BankAccountMockClient client = new BankAccountMockClient();

    @Test
    void timeoutOnFirstAttemptIsRetryable() {
        var request = new IntegrationRequest("r1", Country.SWEDEN, CustomerType.BUSINESS, Map.of("iban", "SE0000000000"), 1);
        var response = client.call(request);
        assertEquals(IntegrationOutcome.FAIL, response.outcome());
        assertTrue(response.retryable());
    }

    @Test
    void secondAttemptOfSameTimeoutIbanSucceeds() {
        var request = new IntegrationRequest("r2", Country.SWEDEN, CustomerType.BUSINESS, Map.of("iban", "SE0000000000"), 2);
        assertEquals(IntegrationOutcome.SUCCESS, client.call(request).outcome());
    }

    @Test
    void nameMismatchIbanFailsAndIsNotRetryable() {
        var request = new IntegrationRequest("r3", Country.SWEDEN, CustomerType.BUSINESS, Map.of("iban", "SE1111111111"), 1);
        var response = client.call(request);
        assertEquals(IntegrationOutcome.FAIL, response.outcome());
        assertFalse(response.retryable());
    }

    @Test
    void ordinaryIbanSucceeds() {
        var request = new IntegrationRequest("r4", Country.SWEDEN, CustomerType.BUSINESS, Map.of("iban", "SE3550000000054910000003"), 1);
        assertEquals(IntegrationOutcome.SUCCESS, client.call(request).outcome());
    }
}
