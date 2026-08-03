# Onboarding flow shape

How the six configured flows (3 countries × 2 customer types) are structured. Shape legend:
▭ = pure data-collection step · ⬡ = data + triggers a mock check · ⬭ = review/submit step.

## Selecting a flow

```mermaid
flowchart LR
    Start(["User picks<br/>country + account type"]) --> Registry{{"FlowRegistry.get(country, type)"}}
    Registry --> SEp["Sweden<br/>Private"]
    Registry --> ESp["Spain<br/>Private"]
    Registry --> PLp["Poland<br/>Private"]
    Registry --> SEb["Sweden<br/>Business"]
    Registry --> ESb["Spain<br/>Business"]
    Registry --> PLb["Poland<br/>Business"]

    SEp & ESp & PLp --> PrivateShape["→ Private flow shape"]
    SEb & ESb & PLb --> BusinessShape["→ Business flow shape"]
```

Only the field set and validation patterns differ between Sweden/Spain/Poland (e.g. personnummer
vs. DNI/NIE vs. PESEL) — the *step shape* is identical within each customer type across all three
markets, because all three providers compose the same kind of steps.

## Private individual flow (identical shape for SE / ES / PL)

```mermaid
flowchart TD
    A["⬡ IDENTITY_VERIFICATION<br/>full name, ID number<br/><i>→ Identity/KYC mock</i>"] --> B["▭ CONTACT_DETAILS<br/>address, postal code, city,<br/>tax residency"]
    B --> C["⬡ CONSENT_DECLARATIONS<br/>consent, PEP declaration<br/><i>→ PEP/sanctions mock</i>"]
    C --> D["▭ FINANCIAL_PROFILE<br/>employment, income,<br/>debt, housing cost"]
    D --> E["⬡ CREDIT_DECISION<br/>(no new input)<br/><i>→ Credit/affordability mock,<br/>using data from step D</i>"]
    E --> F(["⬭ REVIEW_SUBMIT<br/>accept terms → runs DecisionEngine"])
    F --> G{{"APPROVED / MANUAL_REVIEW / REJECTED"}}
```

## Business flow (identical shape for SE / ES / PL)

```mermaid
flowchart TD
    A["⬡ COMPANY_DETAILS<br/>company ID, legal name, legal form<br/><i>→ KYB/registry mock</i>"] --> B["⬡ REPRESENTATIVE_AUTHORITY<br/>representative name, ID,<br/>signatory rights<br/><i>→ Identity/KYC mock</i>"]
    B --> C["⬡ BENEFICIAL_OWNERS<br/>owner name, ownership %, ID<br/><i>→ PEP/sanctions mock</i>"]
    C --> D["▭ BUSINESS_PROFILE<br/>sector, turnover,<br/>monthly volume, purpose"]
    D --> E["⬡ BUSINESS_CREDIT_DECISION<br/>(no new input)<br/><i>→ Credit/affordability mock,<br/>using turnover from step D</i>"]
    E --> F["⬡ BANK_ACCOUNT_VERIFICATION<br/>IBAN<br/><i>→ Bank account mock<br/>(may need a retry, see diagram 6)</i>"]
    F --> G(["⬭ REVIEW_SUBMIT<br/>accept terms → runs DecisionEngine"])
    G --> H{{"APPROVED / MANUAL_REVIEW / REJECTED"}}
```

## Why this shape

- **Business has one more step than private** (`BANK_ACCOUNT_VERIFICATION`) — split out on
  purpose, not folded into the credit step, specifically to give the timeout/retry requirement a
  dedicated, visible place in the journey (see README "assumptions" for the reasoning).
- **Every check step's mock is called with the full data collected so far**, not just that step's
  own fields — e.g. `CREDIT_DECISION` has no form fields of its own; it reuses income/debt/housing
  captured on `FINANCIAL_PROFILE`. This is why `IntegrationRequest.data` is the *aggregated*
  application data, not just the current step's submission (see
  [diagram 3](03-integration-decisioning-class-diagram.md)).
- **The review step is always last and always triggers `DecisionEngine`** — it's the one place
  `StepDefinition.reviewStep == true`, and `OnboardingService` checks that flag rather than
  comparing against a hardcoded step key.
