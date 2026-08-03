package com.ikano.onboarding.service.dto;

import com.ikano.onboarding.domain.ApplicationStatus;
import com.ikano.onboarding.domain.Country;
import com.ikano.onboarding.domain.CustomerType;
import com.ikano.onboarding.flow.StepDefinition;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record StepPageModel(
        UUID applicationId,
        ApplicationStatus applicationStatus,
        Country country,
        CustomerType customerType,
        List<StepDefinition> allSteps,
        List<String> completedStepKeys,
        int currentIndex,
        StepDefinition step,
        Map<String, String> values,
        Map<String, String> fieldErrors,
        String integrationFailureMessage,
        boolean retryable,
        String resumeUrl,
        Map<String, Map<String, String>> reviewData
) {
}
