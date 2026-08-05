package com.ikano.onboarding.web.api.dto;

import com.ikano.onboarding.service.dto.StepPageModel;

import java.util.UUID;

/**
 * Wraps a step submission outcome: on success the caller either gets the next step's page
 * (applicationComplete=false) or just a completion signal (applicationComplete=true, page=null,
 * fetch /result next); on failure page is the current step re-rendered with errors attached.
 */
public record StepSubmitResponse(boolean success, boolean applicationComplete, UUID applicationId, StepPageModel page) {

    public static StepSubmitResponse next(StepPageModel page) {
        return new StepSubmitResponse(true, false, page.applicationId(), page);
    }

    public static StepSubmitResponse complete(UUID applicationId) {
        return new StepSubmitResponse(true, true, applicationId, null);
    }

    public static StepSubmitResponse invalid(StepPageModel pageWithErrors) {
        return new StepSubmitResponse(false, false, pageWithErrors.applicationId(), pageWithErrors);
    }
}
