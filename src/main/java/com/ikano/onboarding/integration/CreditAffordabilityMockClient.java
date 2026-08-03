package com.ikano.onboarding.integration;

import com.ikano.onboarding.domain.IntegrationType;
import org.springframework.stereotype.Component;

/**
 * Mock credit-bureau + affordability decision. For private customers it uses disposable income
 * (income minus debt minus housing cost) from the financial-profile step; for business customers
 * it uses declared annual turnover. Both are simple, explicit, and directly traceable to what the
 * candidate typed in — no hidden magic strings.
 */
@Component
public class CreditAffordabilityMockClient implements IntegrationClient {

    private static final double MANUAL_REVIEW_MARGIN_PRIVATE = 500;
    private static final double MANUAL_REVIEW_TURNOVER_BUSINESS = 50_000;

    @Override
    public IntegrationType type() {
        return IntegrationType.CREDIT_AFFORDABILITY;
    }

    @Override
    public IntegrationResponse call(IntegrationRequest request) {
        if (request.field("annualTurnover") != null) {
            return evaluateBusiness(request);
        }
        return evaluatePrivate(request);
    }

    private IntegrationResponse evaluatePrivate(IntegrationRequest request) {
        double income = MockRules.parseNumber(request.field("monthlyIncome"));
        double debt = MockRules.parseNumber(request.field("monthlyDebtPayments"));
        double housing = MockRules.parseNumber(request.field("housingCost"));
        double disposable = income - debt - housing;

        if (disposable < 0) {
            return IntegrationResponse.fail("insufficient_disposable_income",
                    "Declared expenses exceed income; affordability check failed.");
        }
        if (disposable < MANUAL_REVIEW_MARGIN_PRIVATE) {
            return IntegrationResponse.manualReview("low_affordability_margin",
                    "Disposable income margin is thin and needs manual review.");
        }
        return IntegrationResponse.success("affordability_confirmed", "Affordability check passed.");
    }

    private IntegrationResponse evaluateBusiness(IntegrationRequest request) {
        double turnover = MockRules.parseNumber(request.field("annualTurnover"));

        if (turnover <= 0) {
            return IntegrationResponse.fail("insufficient_turnover", "Declared turnover is missing or not credible.");
        }
        if (turnover < MANUAL_REVIEW_TURNOVER_BUSINESS) {
            return IntegrationResponse.manualReview("low_turnover_margin", "Turnover is below the automatic approval threshold.");
        }
        return IntegrationResponse.success("business_credit_approved", "Business credit and risk check passed.");
    }
}
