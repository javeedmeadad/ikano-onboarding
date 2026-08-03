package com.ikano.onboarding.flow;

import com.ikano.onboarding.domain.Country;
import com.ikano.onboarding.domain.CustomerType;
import com.ikano.onboarding.flow.provider.PolandBusinessFlowProvider;
import com.ikano.onboarding.flow.provider.PolandPrivateFlowProvider;
import com.ikano.onboarding.flow.provider.SpainBusinessFlowProvider;
import com.ikano.onboarding.flow.provider.SpainPrivateFlowProvider;
import com.ikano.onboarding.flow.provider.SwedenBusinessFlowProvider;
import com.ikano.onboarding.flow.provider.SwedenPrivateFlowProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlowRegistryTest {

    private FlowRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new FlowRegistry(List.of(
                new SwedenPrivateFlowProvider(),
                new SpainPrivateFlowProvider(),
                new PolandPrivateFlowProvider(),
                new SwedenBusinessFlowProvider(),
                new SpainBusinessFlowProvider(),
                new PolandBusinessFlowProvider()
        ));
    }

    @Test
    void registersAllSixMarketFlows() {
        assertEquals(6, registry.all().size());
        for (Country country : Country.values()) {
            for (CustomerType type : CustomerType.values()) {
                assertTrue(registry.find(country, type).isPresent(), "Missing flow for " + country + "/" + type);
            }
        }
    }

    @Test
    void everyFlowStartsWithADataOrCheckStepAndEndsWithReview() {
        for (FlowDefinition flow : registry.all()) {
            assertFalse(flow.steps().isEmpty(), "Flow has no steps: " + flow.country() + "/" + flow.customerType());
            assertFalse(flow.firstStep().orElseThrow().reviewStep(), "First step should not be the review step");
            assertTrue(flow.steps().get(flow.steps().size() - 1).reviewStep(), "Last step should be the review step");
        }
    }

    @Test
    void stepKeysAreUniqueWithinAFlow() {
        for (FlowDefinition flow : registry.all()) {
            long distinctKeys = flow.steps().stream().map(StepDefinition::key).distinct().count();
            assertEquals(flow.steps().size(), distinctKeys, "Duplicate step keys in " + flow.country() + "/" + flow.customerType());
        }
    }

    @Test
    void unknownFlowThrows() {
        FlowRegistry empty = new FlowRegistry(List.of());
        try {
            empty.get(Country.SWEDEN, CustomerType.PRIVATE);
            throw new AssertionError("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }
}
