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
public class SwedenBusinessFlowProvider implements FlowDefinitionProvider {

    @Override
    public FlowDefinition define() {
        return new FlowDefinition(Country.SWEDEN, CustomerType.BUSINESS, List.of(
                StepDefinition.checkStep("COMPANY_DETAILS", "Company details",
                        "Collect the organisation number and legal form, then run a Bolagsverket-style registry lookup.",
                        List.of(
                                FieldDefinition.text("legalName", "Registered company name", true),
                                FieldDefinition.text("companyId", "Organisation number", true,
                                        "^\\d{6}-?\\d{4}$", "Use format NNNNNN-NNNN.",
                                        "Demo: number ending in 0 fails, ending in 9 needs manual review."),
                                FieldDefinition.select("legalForm", "Legal form", true,
                                        List.of("LIMITED_COMPANY", "SOLE_TRADER", "PARTNERSHIP", "ECONOMIC_ASSOCIATION"))
                        ), IntegrationType.KYB_REGISTRY),
                StepDefinition.checkStep("REPRESENTATIVE_AUTHORITY", "Representative and signatory rights",
                        "Confirm the authorised representative and verify their identity.",
                        List.of(
                                FieldDefinition.text("repFullName", "Representative full name", true),
                                FieldDefinition.text("repIdNumber", "Representative personal identity number", true,
                                        "^\\d{6,8}-?\\d{4}$", "Use format YYYYMMDD-XXXX.", null),
                                FieldDefinition.checkbox("signatoryRights", "This person has signatory rights for the company", true)
                        ), IntegrationType.IDENTITY_KYC),
                StepDefinition.checkStep("BENEFICIAL_OWNERS", "Beneficial owners",
                        "Collect beneficial owners and verify them with a BankID-style KYC and sanctions mock.",
                        List.of(
                                FieldDefinition.text("uboName", "Beneficial owner full name", true),
                                FieldDefinition.number("uboOwnershipPercent", "Ownership percentage", true),
                                FieldDefinition.text("uboIdNumber", "Beneficial owner identity number", true),
                                FieldDefinition.checkbox("simulateSanctionsHit", "Demo: simulate a possible sanctions hit", false,
                                        "For demo purposes only — simulates what happens on a screening hit.")
                        ), IntegrationType.PEP_SANCTIONS),
                StepDefinition.dataStep("BUSINESS_PROFILE", "Business activity",
                        "Capture business activity, turnover, purpose and expected usage.",
                        List.of(
                                FieldDefinition.select("sector", "Business sector", true,
                                        List.of("RETAIL", "TECHNOLOGY", "HOSPITALITY", "CONSTRUCTION", "PROFESSIONAL_SERVICES", "OTHER")),
                                FieldDefinition.number("annualTurnover", "Expected annual turnover (SEK)", true),
                                FieldDefinition.number("expectedMonthlyVolume", "Expected monthly transaction volume (SEK)", true),
                                FieldDefinition.textarea("purpose", "Purpose of the account", true)
                        )),
                StepDefinition.checkStep("BUSINESS_CREDIT_DECISION", "Business credit and risk check",
                        "Run KYB, sanctions/PEP and business-credit decisioning using the profile you provided.",
                        List.of(), IntegrationType.CREDIT_AFFORDABILITY),
                StepDefinition.checkStep("BANK_ACCOUNT_VERIFICATION", "Bank account verification",
                        "Verify the company's settlement account.",
                        List.of(
                                FieldDefinition.text("iban", "IBAN", true, "^SE\\d{2}[0-9A-Za-z]{4,20}$",
                                        "Use a Swedish IBAN starting with SE.",
                                        "Demo: an IBAN ending in 0000 simulates a timeout — retry to succeed.")
                        ), IntegrationType.BANK_ACCOUNT),
                StepDefinition.reviewStep("REVIEW_SUBMIT", "Review and submit",
                        "Review the application, accept the terms and submit for signing.",
                        List.of(FieldDefinition.checkbox("acceptTerms", "I accept the terms and conditions", true)))
        ));
    }
}
