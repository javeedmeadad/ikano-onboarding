package com.ikano.onboarding.flow.provider;

import com.ikano.onboarding.domain.Country;
import com.ikano.onboarding.domain.CustomerType;
import com.ikano.onboarding.domain.IntegrationType;
import com.ikano.onboarding.flow.FieldDefinition;
import com.ikano.onboarding.flow.FlowDefinition;
import com.ikano.onboarding.flow.FlowDefinitionProvider;
import com.ikano.onboarding.flow.StepDefinition;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SwedenPrivateFlowProvider implements FlowDefinitionProvider {

    @Override
    public FlowDefinition define() {
        return new FlowDefinition(Country.SWEDEN, CustomerType.PRIVATE, List.of(
                StepDefinition.checkStep("IDENTITY_VERIFICATION", "Identity verification",
                        "Collect the personal identity number and run a BankID-style identity check.",
                        List.of(
                                FieldDefinition.text("fullName", "Full legal name", true),
                                FieldDefinition.text("idNumber", "Personal identity number (personnummer)", true,
                                        "^\\d{6,8}-?\\d{4}$", "Use format YYYYMMDD-XXXX.",
                                        "Demo: ID ending in 0 fails, ending in 9 needs manual review.")
                        ), IntegrationType.IDENTITY_KYC),
                StepDefinition.dataStep("CONTACT_DETAILS", "Contact details",
                        "Confirm contact details, address and tax residency.",
                        List.of(
                                FieldDefinition.text("addressLine", "Address", true),
                                FieldDefinition.text("postalCode", "Postal code", true, "^\\d{3}\\s?\\d{2}$", "Use format NNN NN.", null),
                                FieldDefinition.text("city", "City", true),
                                FieldDefinition.text("taxResidency", "Tax residency country", true)
                        )),
                StepDefinition.checkStep("CONSENT_DECLARATIONS", "Consent and declarations",
                        "Capture consent and PEP/sanctions declaration.",
                        List.of(
                                FieldDefinition.checkbox("consentTerms", "I consent to data processing for onboarding purposes", true),
                                FieldDefinition.checkbox("pepDeclaration", "I am a politically exposed person (PEP)", false),
                                FieldDefinition.checkbox("simulateSanctionsHit", "Demo: simulate a possible sanctions hit", false,
                                        "For demo purposes only — simulates what happens on a screening hit.")
                        ), IntegrationType.PEP_SANCTIONS),
                StepDefinition.dataStep("FINANCIAL_PROFILE", "Financial profile",
                        "Employment, income, household and affordability inputs.",
                        List.of(
                                FieldDefinition.select("employmentStatus", "Employment status", true,
                                        List.of("EMPLOYED", "SELF_EMPLOYED", "UNEMPLOYED", "RETIRED", "STUDENT")),
                                FieldDefinition.number("monthlyIncome", "Monthly net income (SEK)", true),
                                FieldDefinition.number("monthlyDebtPayments", "Monthly debt payments (SEK)", true),
                                FieldDefinition.number("housingCost", "Monthly housing cost (SEK)", true),
                                FieldDefinition.number("dependants", "Number of dependants", false)
                        )),
                StepDefinition.checkStep("CREDIT_DECISION", "Credit and affordability check",
                        "Run the credit-bureau and affordability decision using the financial profile you provided.",
                        List.of(), IntegrationType.CREDIT_AFFORDABILITY),
                StepDefinition.reviewStep("REVIEW_SUBMIT", "Review and submit",
                        "Review your answers, accept the terms and submit your application.",
                        List.of(FieldDefinition.checkbox("acceptTerms", "I accept the terms and conditions", true)))
        ));
    }
}
