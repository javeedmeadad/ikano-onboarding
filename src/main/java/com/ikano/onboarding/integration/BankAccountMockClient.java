package com.ikano.onboarding.integration;

import com.ikano.onboarding.domain.IntegrationType;
import org.springframework.stereotype.Component;

/**
 * Mock bank-account/IBAN verification for business onboarding. An IBAN ending in "0000" simulates
 * a transient timeout on the first attempt (retryable=true) and succeeds on retry — demonstrating
 * graceful handling of a flaky downstream dependency. An IBAN ending in "1111" is a deterministic
 * hard failure (name mismatch).
 */
@Component
public class BankAccountMockClient implements IntegrationClient {

    @Override
    public IntegrationType type() {
        return IntegrationType.BANK_ACCOUNT;
    }

    @Override
    public IntegrationResponse call(IntegrationRequest request) {
        String iban = request.field("iban");
        if (iban == null) {
            return IntegrationResponse.fail("iban_missing", "No IBAN supplied.");
        }

        if (iban.endsWith("0000") && request.attempt() < 2) {
            return IntegrationResponse.retryableFail("unreachable", "Bank account service timed out. Please retry.");
        }
        if (iban.endsWith("1111")) {
            return IntegrationResponse.fail("name_mismatch", "Account holder name does not match the applicant.");
        }
        return IntegrationResponse.success("iban_verified", "Bank account verified.");
    }
}
