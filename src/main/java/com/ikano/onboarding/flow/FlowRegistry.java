package com.ikano.onboarding.flow;

import com.ikano.onboarding.domain.Country;
import com.ikano.onboarding.domain.CustomerType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Central lookup for "which flow applies to this country + customer type". Built once at startup
 * from every {@link FlowDefinitionProvider} bean found on the classpath. Nothing in the web,
 * service or decisioning layers branches on {@link Country} or {@link CustomerType} directly —
 * they all go through this registry instead.
 */
@Component
public class FlowRegistry {

    private final Map<FlowKey, FlowDefinition> flows;

    public FlowRegistry(List<FlowDefinitionProvider> providers) {
        this.flows = providers.stream()
                .map(FlowDefinitionProvider::define)
                .collect(Collectors.toMap(f -> new FlowKey(f.country(), f.customerType()), f -> f));
    }

    public FlowDefinition get(Country country, CustomerType customerType) {
        FlowKey key = new FlowKey(country, customerType);
        FlowDefinition flow = flows.get(key);
        if (flow == null) {
            throw new IllegalArgumentException("No flow configured for " + country + " / " + customerType);
        }
        return flow;
    }

    public Optional<FlowDefinition> find(Country country, CustomerType customerType) {
        return Optional.ofNullable(flows.get(new FlowKey(country, customerType)));
    }

    public List<FlowDefinition> all() {
        return List.copyOf(flows.values());
    }
}
