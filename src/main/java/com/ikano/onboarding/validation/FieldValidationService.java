package com.ikano.onboarding.validation;

import com.ikano.onboarding.domain.FieldType;
import com.ikano.onboarding.flow.FieldDefinition;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Validates submitted form data purely from {@link FieldDefinition} metadata. No step or country
 * ever needs a bespoke validator class — required-ness, regex patterns and numeric-ness are all
 * declared on the field definition itself.
 */
@Service
public class FieldValidationService {

    public Map<String, String> validate(List<FieldDefinition> fields, Map<String, String> submitted) {
        Map<String, String> errors = new LinkedHashMap<>();

        for (FieldDefinition field : fields) {
            String value = submitted.getOrDefault(field.name(), "");

            if (field.type() == FieldType.CHECKBOX) {
                boolean checked = "true".equalsIgnoreCase(value) || "on".equalsIgnoreCase(value);
                if (field.required() && !checked) {
                    errors.put(field.name(), field.label() + " is required.");
                }
                continue;
            }

            if (field.required() && value.isBlank()) {
                errors.put(field.name(), field.label() + " is required.");
                continue;
            }

            if (value.isBlank()) {
                continue;
            }

            if (field.type() == FieldType.NUMBER && !isNumeric(value)) {
                errors.put(field.name(), field.label() + " must be a number.");
                continue;
            }

            if (field.pattern() != null && !Pattern.matches(field.pattern(), value)) {
                String message = field.patternErrorMessage() != null
                        ? field.patternErrorMessage()
                        : field.label() + " is not in a valid format.";
                errors.put(field.name(), message);
            }
        }

        return errors;
    }

    private boolean isNumeric(String value) {
        try {
            Double.parseDouble(value.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
