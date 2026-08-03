package com.ikano.onboarding.web;

import com.ikano.onboarding.domain.Country;
import com.ikano.onboarding.domain.CustomerType;
import com.ikano.onboarding.entity.OnboardingApplicationEntity;
import com.ikano.onboarding.service.OnboardingService;
import com.ikano.onboarding.service.dto.StepPageModel;
import com.ikano.onboarding.service.dto.StepSubmissionOutcome;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Controller
public class OnboardingController {

    private final OnboardingService onboardingService;

    public OnboardingController(OnboardingService onboardingService) {
        this.onboardingService = onboardingService;
    }

    @PostMapping("/onboarding/start")
    public String start(@RequestParam Country country, @RequestParam CustomerType customerType) {
        OnboardingApplicationEntity app = onboardingService.start(country, customerType);
        return "redirect:/onboarding/" + app.getId() + "/step";
    }

    @GetMapping("/onboarding/{id}/step")
    public String showCurrentStep(@PathVariable UUID id, Model model) {
        model.addAttribute("page", onboardingService.loadStep(id, null));
        return "step";
    }

    @PostMapping("/onboarding/{id}/step")
    public String submitStep(@PathVariable UUID id, @RequestParam Map<String, String> allParams, Model model) {
        String stepKey = allParams.get("stepKey");
        Map<String, String> formData = new HashMap<>(allParams);
        formData.remove("stepKey");

        StepSubmissionOutcome outcome = onboardingService.submitStep(id, stepKey, formData);

        if (outcome.success()) {
            if (outcome.applicationComplete()) {
                return "redirect:/onboarding/" + id + "/result";
            }
            return "redirect:/onboarding/" + id + "/step";
        }

        StepPageModel base = onboardingService.loadStep(id, stepKey);
        StepPageModel withErrors = new StepPageModel(base.applicationId(), base.applicationStatus(), base.country(),
                base.customerType(), base.allSteps(), base.completedStepKeys(), base.currentIndex(), base.step(),
                formData, outcome.fieldErrors(), outcome.integrationFailureMessage(), outcome.retryable(),
                base.resumeUrl(), base.reviewData());
        model.addAttribute("page", withErrors);
        return "step";
    }

    @GetMapping("/onboarding/{id}/result")
    public String result(@PathVariable UUID id, Model model) {
        model.addAttribute("summary", onboardingService.summary(id));
        return "result";
    }
}
