package com.ikano.onboarding.decision;

import com.ikano.onboarding.domain.DecisionOutcome;
import com.ikano.onboarding.domain.IntegrationOutcome;
import com.ikano.onboarding.entity.IntegrationResultEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Single, country- and customer-type-agnostic decisioning rule. Because every mock integration
 * already normalizes its result to {@link IntegrationOutcome}, this engine never needs to know
 * whether it is looking at a Swedish private applicant or a Polish business — it only needs the
 * list of check outcomes. Precedence: any FAIL wins (reject), else any MANUAL_REVIEW wins, else
 * approve. This is what "add a country without rewriting the app" means in practice.
 */
@Component
public class DecisionEngine {

    public DecisionResult decide(List<IntegrationResultEntity> results) {
        List<IntegrationResultEntity> failures = results.stream()
                .filter(r -> r.getOutcome() == IntegrationOutcome.FAIL)
                .toList();
        if (!failures.isEmpty()) {
            String reason = "Rejected due to: " + describe(failures);
            return new DecisionResult(DecisionOutcome.REJECTED, reason);
        }

        List<IntegrationResultEntity> manualReview = results.stream()
                .filter(r -> r.getOutcome() == IntegrationOutcome.MANUAL_REVIEW)
                .toList();
        if (!manualReview.isEmpty()) {
            String reason = "Referred to manual review due to: " + describe(manualReview);
            return new DecisionResult(DecisionOutcome.MANUAL_REVIEW, reason);
        }

        return new DecisionResult(DecisionOutcome.APPROVED, "All checks passed automatically.");
    }

    private String describe(List<IntegrationResultEntity> results) {
        return results.stream()
                .map(r -> r.getIntegrationType() + " (" + r.getDetailCode() + ")")
                .collect(Collectors.joining(", "));
    }

    public record DecisionResult(DecisionOutcome outcome, String reason) {
    }
}
