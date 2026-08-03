package com.ikano.onboarding.domain;

/**
 * Normalized tri-state result every mock integration collapses to, regardless of the
 * service-specific detail code (e.g. "expired_id", "dissolved", "confirmed_hit" all map to FAIL).
 * This normalization is what lets {@link com.ikano.onboarding.decision.DecisionEngine} stay a
 * single, country-agnostic rule set instead of growing a branch per integration type.
 */
public enum IntegrationOutcome {
    SUCCESS,
    MANUAL_REVIEW,
    FAIL
}
