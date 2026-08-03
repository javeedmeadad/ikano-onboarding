package com.ikano.onboarding.flow;

import com.ikano.onboarding.domain.Country;
import com.ikano.onboarding.domain.CustomerType;

public record FlowKey(Country country, CustomerType customerType) {
}
