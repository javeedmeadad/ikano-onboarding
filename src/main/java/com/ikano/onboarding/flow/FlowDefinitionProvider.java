package com.ikano.onboarding.flow;

/**
 * Contract for a single market's flow. Implementations are picked up automatically by
 * {@link FlowRegistry} via Spring component scanning — supporting a new country or customer
 * type is adding one new {@code @Component} class, not editing existing code (open/closed).
 */
public interface FlowDefinitionProvider {

    FlowDefinition define();
}
