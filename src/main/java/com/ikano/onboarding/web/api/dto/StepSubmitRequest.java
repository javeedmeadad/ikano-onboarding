package com.ikano.onboarding.web.api.dto;

import java.util.Map;

public record StepSubmitRequest(String stepKey, Map<String, String> values) {
}
