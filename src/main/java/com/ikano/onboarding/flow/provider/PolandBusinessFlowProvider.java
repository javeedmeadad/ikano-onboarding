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
public class PolandBusinessFlowProvider implements FlowDefinitionProvider {

    @Override
    public FlowDefinition define() {
        return new FlowDefinition(Country.POLAND, CustomerType.BUSINESS, List.of(
                StepDefinition.checkStep("COMPANY_DETAILS", "Company details",
                        "Collect the NIP/REGON/KRS identifier and legal form, then run a CEIDG/KRS-style lookup.",
                        List.of(
                                FieldDefinition.text("legalName", "Registered company name", true),
                                FieldDefinition.text("companyId", "NIP", true,
                                        "^\\d{10}$", "NIP must be 10 digits.",
                                        "Demo: number ending in 0 fails, ending in 9 needs manual review."),
                                FieldDefinition.select("legalForm", "Legal form", true,
                                        List.of("SP_Z_O_O", "SOLE_PROPRIETOR", "SPOLKA_AKCYJNA", "PARTNERSHIP"))
                        ), IntegrationType.KYB_REGISTRY),
                StepDefinition.checkStep("REPRESENTATIVE_AUTHORITY", "Board member / sole proprietor authority",
                        "Confirm authority to act on behalf of the company.",
                        List.of(
                                FieldDefinition.text("repFullName", "Representative full name", true),
                                FieldDefinition.text("repIdNumber", "Representative PESEL", true,
                                        "^\\d{11}$", "PESEL must be 11 digits.", null),
                                FieldDefinition.checkbox("signatoryRights", "This person has signatory rights for the company", true)
                        ), IntegrationType.IDENTITY_KYC),
                StepDefinition.checkStep("BENEFICIAL_OWNERS", "Beneficial owners",
                        "Collect beneficial owners and verify their identity and risk.",
                        List.of(
                                FieldDefinition.text("uboName", "Beneficial owner full name", true),
                                FieldDefinition.number("uboOwnershipPercent", "Ownership percentage", true),
                                FieldDefinition.text("uboIdNumber", "Beneficial owner identity number", true),
                                FieldDefinition.checkbox("simulateSanctionsHit", "Demo: simulate a possible sanctions hit", false,
                                        "For demo purposes only — simulates what happens on a screening hit.")
                        ), IntegrationType.PEP_SANCTIONS),
                StepDefinition.dataStep("BUSINESS_PROFILE", "Business activity",
                        "Capture VAT/tax status, business activity and expected usage.",
                        List.of(
                                FieldDefinition.select("sector", "Business sector", true,
                                        List.of("RETAIL", "TECHNOLOGY", "HOSPITALITY", "CONSTRUCTION", "PROFESSIONAL_SERVICES", "OTHER")),
                                FieldDefinition.text("vatNumber", "VAT status / number", true),
                                FieldDefinition.number("annualTurnover", "Expected annual turnover (PLN)", true),
                                FieldDefinition.number("expectedMonthlyVolume", "Expected monthly transaction volume (PLN)", true),
                                FieldDefinition.textarea("purpose", "Purpose of the account", true)
                        )),
                StepDefinition.checkStep("BUSINESS_CREDIT_DECISION", "Business credit and risk check",
                        "Run a BIK-style business credit check, sanctions/PEP and KYB decisioning.",
                        List.of(), IntegrationType.CREDIT_AFFORDABILITY),
                StepDefinition.checkStep("BANK_ACCOUNT_VERIFICATION", "Bank account verification",
                        "Verify the company's settlement account.",
                        List.of(
                                FieldDefinition.text("iban", "IBAN", true, "^PL\\d{2}[0-9A-Za-z]{4,20}$",
                                        "Use a Polish IBAN starting with PL.",
                                        "Demo: an IBAN ending in 0000 simulates a timeout — retry to succeed.")
                        ), IntegrationType.BANK_ACCOUNT),
                StepDefinition.reviewStep("REVIEW_SUBMIT", "Review and submit",
                        "Review the application, accept the terms and submit for signing.",
                        List.of(FieldDefinition.checkbox("acceptTerms", "I accept the terms and conditions", true)))
        ));
    }
}
