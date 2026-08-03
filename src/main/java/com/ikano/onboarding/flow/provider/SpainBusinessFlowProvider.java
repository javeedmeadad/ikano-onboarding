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
public class SpainBusinessFlowProvider implements FlowDefinitionProvider {

    @Override
    public FlowDefinition define() {
        return new FlowDefinition(Country.SPAIN, CustomerType.BUSINESS, List.of(
                StepDefinition.checkStep("COMPANY_DETAILS", "Company details",
                        "Collect the company NIF and legal form, then run a Registro Mercantil-style lookup.",
                        List.of(
                                FieldDefinition.text("legalName", "Registered company name", true),
                                FieldDefinition.text("companyId", "Company NIF", true,
                                        "^[A-Za-z0-9]\\d{7}[A-Za-z0-9]$", "Use NIF format.",
                                        "Demo: number ending in 0 fails, ending in 9 needs manual review."),
                                FieldDefinition.select("legalForm", "Legal form", true,
                                        List.of("SOCIEDAD_LIMITADA", "AUTONOMO", "SOCIEDAD_ANONIMA", "COOPERATIVA"))
                        ), IntegrationType.KYB_REGISTRY),
                StepDefinition.checkStep("REPRESENTATIVE_AUTHORITY", "Legal representative",
                        "Verify the legal representative using a DNI/NIE identity mock.",
                        List.of(
                                FieldDefinition.text("repFullName", "Representative full name", true),
                                FieldDefinition.text("repIdNumber", "Representative DNI / NIE", true,
                                        "^[XYZxyz]?\\d{7,8}[A-Za-z]$", "Use DNI or NIE format.", null),
                                FieldDefinition.checkbox("signatoryRights", "This person has signatory rights for the company", true)
                        ), IntegrationType.IDENTITY_KYC),
                StepDefinition.checkStep("BENEFICIAL_OWNERS", "Beneficial owners",
                        "Collect beneficial owners and ownership percentages, and screen them.",
                        List.of(
                                FieldDefinition.text("uboName", "Beneficial owner full name", true),
                                FieldDefinition.number("uboOwnershipPercent", "Ownership percentage", true),
                                FieldDefinition.text("uboIdNumber", "Beneficial owner identity number", true),
                                FieldDefinition.checkbox("simulateSanctionsHit", "Demo: simulate a possible sanctions hit", false,
                                        "For demo purposes only — simulates what happens on a screening hit.")
                        ), IntegrationType.PEP_SANCTIONS),
                StepDefinition.dataStep("BUSINESS_PROFILE", "Business activity",
                        "Capture sector, turnover, VAT/tax details and expected usage.",
                        List.of(
                                FieldDefinition.select("sector", "Business sector", true,
                                        List.of("RETAIL", "TECHNOLOGY", "HOSPITALITY", "CONSTRUCTION", "PROFESSIONAL_SERVICES", "OTHER")),
                                FieldDefinition.text("vatNumber", "VAT number", true),
                                FieldDefinition.number("annualTurnover", "Expected annual turnover (EUR)", true),
                                FieldDefinition.number("expectedMonthlyVolume", "Expected monthly transaction volume (EUR)", true),
                                FieldDefinition.textarea("purpose", "Purpose of the account", true)
                        )),
                StepDefinition.checkStep("BUSINESS_CREDIT_DECISION", "Business credit and risk check",
                        "Run KYB, sanctions/PEP and business-credit decisioning using the profile you provided.",
                        List.of(), IntegrationType.CREDIT_AFFORDABILITY),
                StepDefinition.checkStep("BANK_ACCOUNT_VERIFICATION", "IBAN verification",
                        "Verify the company's settlement account.",
                        List.of(
                                FieldDefinition.text("iban", "IBAN", true, "^ES\\d{2}[0-9A-Za-z]{4,20}$",
                                        "Use a Spanish IBAN starting with ES.",
                                        "Demo: an IBAN ending in 0000 simulates a timeout — retry to succeed.")
                        ), IntegrationType.BANK_ACCOUNT),
                StepDefinition.reviewStep("REVIEW_SUBMIT", "Review and submit",
                        "Review the application, accept the terms and submit for signing.",
                        List.of(FieldDefinition.checkbox("acceptTerms", "I accept the terms and conditions", true)))
        ));
    }
}
