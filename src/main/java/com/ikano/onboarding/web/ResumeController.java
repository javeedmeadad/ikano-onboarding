package com.ikano.onboarding.web;

import com.ikano.onboarding.domain.ApplicationStatus;
import com.ikano.onboarding.entity.OnboardingApplicationEntity;
import com.ikano.onboarding.service.OnboardingService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Optional;

@Controller
public class ResumeController {

    private final OnboardingService onboardingService;

    public ResumeController(OnboardingService onboardingService) {
        this.onboardingService = onboardingService;
    }

    @GetMapping("/resume/{token}")
    public String resume(@PathVariable String token, Model model) {
        Optional<OnboardingApplicationEntity> appOpt = onboardingService.resolveResumeToken(token);
        if (appOpt.isEmpty()) {
            model.addAttribute("reason", "This resume link is not recognised.");
            return "resume-invalid";
        }

        OnboardingApplicationEntity app = appOpt.get();
        if (app.getStatus() == ApplicationStatus.IN_PROGRESS) {
            return "redirect:/onboarding/" + app.getId() + "/step";
        }
        if (app.getStatus() == ApplicationStatus.EXPIRED || app.getStatus() == ApplicationStatus.ABANDONED) {
            model.addAttribute("reason", "This resume link has expired. Please start a new application.");
            return "resume-invalid";
        }
        return "redirect:/onboarding/" + app.getId() + "/result";
    }
}
