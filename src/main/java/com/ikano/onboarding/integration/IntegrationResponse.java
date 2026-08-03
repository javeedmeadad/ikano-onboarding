package com.ikano.onboarding.integration;

import com.ikano.onboarding.domain.IntegrationOutcome;

/**
 * Typed output from a mock integration client. {@code detailCode} is the service-specific reason
 * (e.g. "expired_id", "dissolved", "possible_hit") kept for the audit trail; {@code outcome} is
 * the normalized tri-state the decision engine actually reasons about. {@code retryable} flags a
 * transient failure (e.g. a simulated timeout) the caller may resubmit without penalty.
 */
public record IntegrationResponse(
        IntegrationOutcome outcome,
        String detailCode,
        String summary,
        boolean retryable
) {

    public static IntegrationResponse success(String detailCode, String summary) {
        return new IntegrationResponse(IntegrationOutcome.SUCCESS, detailCode, summary, false);
    }

    public static IntegrationResponse manualReview(String detailCode, String summary) {
        return new IntegrationResponse(IntegrationOutcome.MANUAL_REVIEW, detailCode, summary, false);
    }

    public static IntegrationResponse fail(String detailCode, String summary) {
        return new IntegrationResponse(IntegrationOutcome.FAIL, detailCode, summary, false);
    }

    public static IntegrationResponse retryableFail(String detailCode, String summary) {
        return new IntegrationResponse(IntegrationOutcome.FAIL, detailCode, summary, true);
    }
}
