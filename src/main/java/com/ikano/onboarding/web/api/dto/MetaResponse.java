package com.ikano.onboarding.web.api.dto;

import com.ikano.onboarding.domain.Country;
import com.ikano.onboarding.domain.CustomerType;

public record MetaResponse(Country[] countries, CustomerType[] customerTypes) {
}
