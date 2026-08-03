package com.ikano.onboarding.integration;

import com.ikano.onboarding.domain.IntegrationType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class IntegrationClientRegistry {

    private final Map<IntegrationType, IntegrationClient> clients;

    public IntegrationClientRegistry(List<IntegrationClient> clients) {
        this.clients = clients.stream().collect(Collectors.toMap(IntegrationClient::type, Function.identity()));
    }

    public IntegrationResponse call(IntegrationType type, IntegrationRequest request) {
        IntegrationClient client = clients.get(type);
        if (client == null) {
            throw new IllegalStateException("No mock client registered for " + type);
        }
        return client.call(request);
    }
}
