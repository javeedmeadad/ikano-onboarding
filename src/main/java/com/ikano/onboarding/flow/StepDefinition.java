package com.ikano.onboarding.flow;

import com.ikano.onboarding.domain.IntegrationType;

import java.util.List;
import java.util.Optional;

/**
 * One step in a flow: the fields it collects, and optionally the external check it triggers once
 * the user submits it. A step with no {@code integrationType} is a pure data-collection step
 * (e.g. contact details); a step with an integrationType always collects data first, then calls
 * the mock client with that data as input.
 */
public record StepDefinition(
        String key,
        String title,
        String description,
        List<FieldDefinition> fields,
        IntegrationType integrationType,
        boolean reviewStep
) {

    public static StepDefinition dataStep(String key, String title, String description, List<FieldDefinition> fields) {
        return new StepDefinition(key, title, description, fields, null, false);
    }

    public static StepDefinition checkStep(String key, String title, String description, List<FieldDefinition> fields, IntegrationType integrationType) {
        return new StepDefinition(key, title, description, fields, integrationType, false);
    }

    public static StepDefinition reviewStep(String key, String title, String description, List<FieldDefinition> fields) {
        return new StepDefinition(key, title, description, fields, null, true);
    }

    public Optional<IntegrationType> integration() {
        return Optional.ofNullable(integrationType);
    }
}
