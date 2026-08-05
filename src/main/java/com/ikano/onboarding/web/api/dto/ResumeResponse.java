package com.ikano.onboarding.web.api.dto;

import java.util.UUID;

public record ResumeResponse(ResumeStatus status, UUID applicationId, String reason) {

    public enum ResumeStatus {
        IN_PROGRESS,
        FINISHED,
        INVALID
    }

    public static ResumeResponse invalid(String reason) {
        return new ResumeResponse(ResumeStatus.INVALID, null, reason);
    }

    public static ResumeResponse inProgress(UUID applicationId) {
        return new ResumeResponse(ResumeStatus.IN_PROGRESS, applicationId, null);
    }

    public static ResumeResponse finished(UUID applicationId) {
        return new ResumeResponse(ResumeStatus.FINISHED, applicationId, null);
    }
}
