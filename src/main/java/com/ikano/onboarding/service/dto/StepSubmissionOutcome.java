package com.ikano.onboarding.service.dto;

import java.util.Map;

public record StepSubmissionOutcome(
        boolean success,
        Map<String, String> fieldErrors,
        String integrationFailureMessage,
        boolean retryable,
        boolean applicationComplete
) {

    public static StepSubmissionOutcome invalid(Map<String, String> fieldErrors) {
        return new StepSubmissionOutcome(false, fieldErrors, null, false, false);
    }

    public static StepSubmissionOutcome integrationFailure(String message, boolean retryable) {
        return new StepSubmissionOutcome(false, Map.of(), message, retryable, false);
    }

    public static StepSubmissionOutcome advanced() {
        return new StepSubmissionOutcome(true, Map.of(), null, false, false);
    }

    public static StepSubmissionOutcome completed() {
        return new StepSubmissionOutcome(true, Map.of(), null, false, true);
    }
}
