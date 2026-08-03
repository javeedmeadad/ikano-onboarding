package com.ikano.onboarding.flow;

import com.ikano.onboarding.domain.FieldType;

import java.util.List;

/**
 * Describes one input on a step form. The web layer renders a single generic template off this
 * definition instead of a per-country/per-step template, and {@code FieldValidationService}
 * validates against it generically. Adding a field to a market never touches the controller or
 * the template.
 */
public record FieldDefinition(
        String name,
        String label,
        FieldType type,
        boolean required,
        String pattern,
        String patternErrorMessage,
        List<String> options,
        String helpText
) {

    public static FieldDefinition text(String name, String label, boolean required) {
        return new FieldDefinition(name, label, FieldType.TEXT, required, null, null, List.of(), null);
    }

    public static FieldDefinition text(String name, String label, boolean required, String pattern, String patternErrorMessage, String helpText) {
        return new FieldDefinition(name, label, FieldType.TEXT, required, pattern, patternErrorMessage, List.of(), helpText);
    }

    public static FieldDefinition number(String name, String label, boolean required) {
        return new FieldDefinition(name, label, FieldType.NUMBER, required, null, null, List.of(), null);
    }

    public static FieldDefinition number(String name, String label, boolean required, String helpText) {
        return new FieldDefinition(name, label, FieldType.NUMBER, required, null, null, List.of(), helpText);
    }

    public static FieldDefinition select(String name, String label, boolean required, List<String> options) {
        return new FieldDefinition(name, label, FieldType.SELECT, required, null, null, options, null);
    }

    public static FieldDefinition checkbox(String name, String label, boolean required) {
        return new FieldDefinition(name, label, FieldType.CHECKBOX, required, null, null, List.of(), null);
    }

    public static FieldDefinition checkbox(String name, String label, boolean required, String helpText) {
        return new FieldDefinition(name, label, FieldType.CHECKBOX, required, null, null, List.of(), helpText);
    }

    public static FieldDefinition textarea(String name, String label, boolean required) {
        return new FieldDefinition(name, label, FieldType.TEXTAREA, required, null, null, List.of(), null);
    }
}
