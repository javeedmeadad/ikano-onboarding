package com.ikano.onboarding.integration;

import com.ikano.onboarding.domain.Country;
import com.ikano.onboarding.domain.CustomerType;

import java.util.Map;

/**
 * Typed input to a mock integration client. {@code data} holds every field collected so far in
 * the application (not just the triggering step) so a check can use earlier answers — e.g. the
 * credit check uses income/debt captured on the financial-profile step. {@code attempt} lets a
 * client simulate a transient failure that clears on retry (see {@link BankAccountMockClient}).
 */
public record IntegrationRequest(
        String requestId,
        Country country,
        CustomerType customerType,
        Map<String, String> data,
        int attempt
) {

    public String field(String name) {
        return data.get(name);
    }
}
