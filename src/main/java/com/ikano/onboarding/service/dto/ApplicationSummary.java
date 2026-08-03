package com.ikano.onboarding.service.dto;

import com.ikano.onboarding.entity.AuditEntryEntity;
import com.ikano.onboarding.entity.IntegrationResultEntity;
import com.ikano.onboarding.entity.OnboardingApplicationEntity;
import com.ikano.onboarding.entity.StepRecordEntity;

import java.util.List;
import java.util.Map;

public record ApplicationSummary(
        OnboardingApplicationEntity application,
        List<StepRecordEntity> steps,
        Map<String, Map<String, String>> stepData,
        List<IntegrationResultEntity> integrationResults,
        List<AuditEntryEntity> auditTrail
) {
}
