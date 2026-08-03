package com.ikano.onboarding.validation;

import com.ikano.onboarding.flow.FieldDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FieldValidationServiceTest {

    private final FieldValidationService service = new FieldValidationService();

    @Test
    void missingRequiredTextFieldProducesError() {
        var fields = List.of(FieldDefinition.text("fullName", "Full name", true));
        var errors = service.validate(fields, Map.of());
        assertTrue(errors.containsKey("fullName"));
    }

    @Test
    void patternMismatchProducesError() {
        var fields = List.of(FieldDefinition.text("idNumber", "PESEL", true, "^\\d{11}$", "Must be 11 digits", null));
        var errors = service.validate(fields, Map.of("idNumber", "123"));
        assertTrue(errors.containsKey("idNumber"));
    }

    @Test
    void validPatternPasses() {
        var fields = List.of(FieldDefinition.text("idNumber", "PESEL", true, "^\\d{11}$", "Must be 11 digits", null));
        var errors = service.validate(fields, Map.of("idNumber", "12345678901"));
        assertTrue(errors.isEmpty());
    }

    @Test
    void nonNumericValueForNumberFieldProducesError() {
        var fields = List.of(FieldDefinition.number("monthlyIncome", "Monthly income", true));
        var errors = service.validate(fields, Map.of("monthlyIncome", "not-a-number"));
        assertTrue(errors.containsKey("monthlyIncome"));
    }

    @Test
    void requiredCheckboxMustBeChecked() {
        var fields = List.of(FieldDefinition.checkbox("acceptTerms", "Accept terms", true));
        assertTrue(service.validate(fields, Map.of()).containsKey("acceptTerms"));
        assertFalse(service.validate(fields, Map.of("acceptTerms", "true")).containsKey("acceptTerms"));
    }

    @Test
    void optionalBlankFieldIsFine() {
        var fields = List.of(FieldDefinition.number("dependants", "Dependants", false));
        assertTrue(service.validate(fields, Map.of()).isEmpty());
    }
}
