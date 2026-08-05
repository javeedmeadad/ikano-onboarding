package com.ikano.onboarding.web.api;

import com.ikano.onboarding.domain.ApplicationStatus;
import com.ikano.onboarding.domain.Country;
import com.ikano.onboarding.domain.CustomerType;
import com.ikano.onboarding.entity.OnboardingApplicationEntity;
import com.ikano.onboarding.service.OnboardingService;
import com.ikano.onboarding.service.dto.ApplicationSummary;
import com.ikano.onboarding.service.dto.StepPageModel;
import com.ikano.onboarding.service.dto.StepSubmissionOutcome;
import com.ikano.onboarding.web.api.dto.MetaResponse;
import com.ikano.onboarding.web.api.dto.ResumeResponse;
import com.ikano.onboarding.web.api.dto.StartRequest;
import com.ikano.onboarding.web.api.dto.StepSubmitRequest;
import com.ikano.onboarding.web.api.dto.StepSubmitResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.UUID;

/**
 * JSON API consumed by the AngularJS SPA (static/). Thin wrapper around {@link OnboardingService};
 * carries no business logic of its own — same contract the old Thymeleaf controllers rendered,
 * just serialized instead of rendered server-side.
 */
@RestController
public class OnboardingApiController {

    private final OnboardingService onboardingService;

    public OnboardingApiController(OnboardingService onboardingService) {
        this.onboardingService = onboardingService;
    }

    @GetMapping("/api/meta")
    public MetaResponse meta() {
        return new MetaResponse(Country.values(), CustomerType.values());
    }

    @PostMapping("/api/applications")
    public StepPageModel start(@RequestBody StartRequest request) {
        OnboardingApplicationEntity app = onboardingService.start(request.country(), request.customerType());
        return onboardingService.loadStep(app.getId(), null);
    }

    @GetMapping("/api/applications/{id}/step")
    public StepPageModel currentStep(@PathVariable UUID id) {
        return onboardingService.loadStep(id, null);
    }

    @PostMapping("/api/applications/{id}/step")
    public StepSubmitResponse submitStep(@PathVariable UUID id, @RequestBody StepSubmitRequest request) {
        StepSubmissionOutcome outcome = onboardingService.submitStep(id, request.stepKey(), request.values());

        if (outcome.success()) {
            if (outcome.applicationComplete()) {
                return StepSubmitResponse.complete(id);
            }
            return StepSubmitResponse.next(onboardingService.loadStep(id, null));
        }

        StepPageModel base = onboardingService.loadStep(id, request.stepKey());
        StepPageModel withErrors = new StepPageModel(base.applicationId(), base.applicationStatus(), base.country(),
                base.customerType(), base.allSteps(), base.completedStepKeys(), base.currentIndex(), base.step(),
                request.values(), outcome.fieldErrors(), outcome.integrationFailureMessage(), outcome.retryable(),
                base.resumeUrl(), base.reviewData());
        return StepSubmitResponse.invalid(withErrors);
    }

    @GetMapping("/api/applications/{id}/result")
    public ApplicationSummary result(@PathVariable UUID id) {
        return onboardingService.summary(id);
    }

    @GetMapping("/api/resume/{token}")
    public ResumeResponse resume(@PathVariable String token) {
        Optional<OnboardingApplicationEntity> appOpt = onboardingService.resolveResumeToken(token);
        if (appOpt.isEmpty()) {
            return ResumeResponse.invalid("This resume link is not recognised.");
        }

        OnboardingApplicationEntity app = appOpt.get();
        if (app.getStatus() == ApplicationStatus.IN_PROGRESS) {
            return ResumeResponse.inProgress(app.getId());
        }
        if (app.getStatus() == ApplicationStatus.EXPIRED || app.getStatus() == ApplicationStatus.ABANDONED) {
            return ResumeResponse.invalid("This resume link has expired. Please start a new application.");
        }
        return ResumeResponse.finished(app.getId());
    }
}
