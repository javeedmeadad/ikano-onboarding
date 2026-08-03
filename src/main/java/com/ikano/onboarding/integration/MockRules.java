package com.ikano.onboarding.integration;

/** Shared helpers for the deterministic mock clients. */
final class MockRules {

    private MockRules() {
    }

    static char lastDigit(String value) {
        if (value == null) {
            return ' ';
        }
        for (int i = value.length() - 1; i >= 0; i--) {
            char c = value.charAt(i);
            if (Character.isDigit(c)) {
                return c;
            }
        }
        return ' ';
    }

    static double parseNumber(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
