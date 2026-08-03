package com.ikano.onboarding.flow;

import com.ikano.onboarding.domain.Country;
import com.ikano.onboarding.domain.CustomerType;

import java.util.List;
import java.util.Optional;

public record FlowDefinition(
        Country country,
        CustomerType customerType,
        List<StepDefinition> steps
) {

    public Optional<StepDefinition> step(String key) {
        return steps.stream().filter(s -> s.key().equals(key)).findFirst();
    }

    public int indexOf(String key) {
        for (int i = 0; i < steps.size(); i++) {
            if (steps.get(i).key().equals(key)) {
                return i;
            }
        }
        throw new IllegalArgumentException("Unknown step key: " + key);
    }

    public Optional<StepDefinition> firstStep() {
        return steps.isEmpty() ? Optional.empty() : Optional.of(steps.get(0));
    }

    public Optional<StepDefinition> nextStep(String currentKey) {
        int idx = indexOf(currentKey);
        return idx + 1 < steps.size() ? Optional.of(steps.get(idx + 1)) : Optional.empty();
    }

    public boolean isLastStep(String key) {
        return indexOf(key) == steps.size() - 1;
    }
}
