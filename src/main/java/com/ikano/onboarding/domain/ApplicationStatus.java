package com.ikano.onboarding.domain;

/**
 * Lifecycle of an onboarding application. Distinct states for abandoned / expired / submitted
 * / manually reviewed applications, per the resumability requirement.
 */
public enum ApplicationStatus {
    IN_PROGRESS,
    SUBMITTED,
    APPROVED,
    MANUAL_REVIEW,
    REJECTED,
    ABANDONED,
    EXPIRED
}
