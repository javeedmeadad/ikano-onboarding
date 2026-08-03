# Ikano-style onboarding sample

A small Spring Boot web app demonstrating an adaptable customer onboarding flow across three
markets (Sweden, Spain, Poland) and two customer types (private individual, business), with
mocked KYC/KYB, sanctions, credit and bank-account checks, and a resumable, audited application
lifecycle. Built for the Ikano Java engineering take-home task.

## Stack

- Java 17 (LTS). The task suggests any supported LTS release; 21 wasn't available in this
  environment, so the project targets 17 — bumping `java.version` in `pom.xml` to 21 is a
  one-line change, nothing in the code depends on newer language features.
- Spring Boot 3.3 (Web, Thymeleaf, Data JPA, Validation), Maven
- H2 (file-based, so data survives restarts) — swappable for PostgreSQL by changing four
  properties in `application.properties` and the driver dependency in `pom.xml`
- JUnit 5

## Running it

```bash
mvn spring-boot:run
```

Then open http://localhost:8080/. Data lives in `./data/onboarding.mv.db` (gitignored); delete
that file to reset. The H2 console is at http://localhost:8080/h2-console
(JDBC URL `jdbc:h2:file:./data/onboarding`, user `sa`, empty password).

```bash
mvn test
```

runs the full test suite (34 tests: flow registry, decision engine, each mock client, field
validation, and an end-to-end orchestration test against a real H2-in-memory database).

## Walking through a flow

1. Pick a country and account type on the home page — this selects one of six configured flows.
2. Each step form is rendered generically from that flow's field definitions; submitting a step
   validates the fields, then (if the step has one) calls the relevant mock integration.
3. The final "Review and submit" step aggregates every check performed so far into one decision:
   **approved**, **manual review**, or **rejected**.
4. The result page shows the decision and reason, every check performed (with a request ID), the
   audit trail, and everything you entered.
5. Every step page shows a "resume later" magic link (`/resume/{token}`, valid 24h) — use it to
   drop off and pick the application back up at the same step.

## Demo values — how to trigger each mock outcome

All external checks are deterministic mocks (no network calls). To make every outcome reliably
reproducible during review, each mock reacts to specific, documented input rather than random
chance:

| Check | How to trigger SUCCESS | How to trigger MANUAL REVIEW | How to trigger FAIL |
|---|---|---|---|
| Identity / KYC (personal ID, or representative ID in business flows) | ID number ending in any digit except 0 or 9 | ID number ending in **9** | ID number ending in **0** |
| KYB / company registry | Company ID ending in any digit except 0 or 9 | Company ID ending in **9** | Company ID ending in **0** |
| PEP / sanctions | Leave "simulate a sanctions hit" unchecked | Check "simulate a sanctions hit" | Name field containing "sanctioned" (case-insensitive) |
| Credit / affordability (private) | Disposable income (income − debt − housing) ≥ 500 | Disposable income between 0 and 500 | Disposable income < 0 |
| Credit / affordability (business) | Annual turnover ≥ 50,000 | Annual turnover between 0 and 50,000 | Annual turnover ≤ 0 |
| Bank account (business only) | Any IBAN not ending in 0000/1111 | — (no manual-review state modelled) | IBAN ending in **0000** → simulated timeout, **retryable** — resubmit the same step and it succeeds on the 2nd attempt. IBAN ending in **1111** → hard failure (name mismatch), not retryable. |

A non-retryable FAIL keeps the applicant on the same step with an error banner and their answers
preserved — they correct the input (e.g. a mistyped ID number) and resubmit. A retryable FAIL
(the bank-timeout case) shows the same pattern but the message frames it as transient.
MANUAL_REVIEW never blocks progress; it's recorded and factored into the final decision.

## Assumptions and deliberate simplifications

Documented here per the task's "we care more about explicit tradeoffs than legal perfection":

- **ID/company number formats are illustrative**, not legally exact (e.g. NIF/PESEL/personnummer
  regexes are simplified). No claim of country compliance is made.
- **Business flow combines a few of the suggested sub-checks.** The brief lists company registry,
  representative/signatory verification, UBO KYC+sanctions, and business credit/risk as four
  separate checks — implemented here as four steps/integrations. Bank-account/IBAN verification
  (mentioned in the Spain table and the general mock-service table) was pulled into its own step
  for every business flow rather than folded into the credit step, specifically so the
  timeout/retry requirement has a dedicated, visible place to demonstrate itself.
- **UBO screening is modelled as a single owner**, not a repeatable list. A real product would let
  a business declare N beneficial owners; this sample collects one to keep the take-home in scope
  and notes it as the first thing to generalize (the field-driven form model already supports
  turning this into a repeatable sub-section without touching the flow engine's contract).
- **Select-field options aren't cross-checked against the declared list server-side** beyond
  "required" — a production version would reject an option that isn't in the configured list.
- **No abandonment sweep.** Applications aren't proactively marked `ABANDONED` on a timer; they
  transition to `EXPIRED` lazily, the moment someone visits an expired resume link. A production
  system would run a scheduled job to sweep genuinely abandoned applications for reporting.
- **Decisioning precedence is FAIL > MANUAL_REVIEW > APPROVED** across all checks, uniformly. This
  is the simplest defensible rule and keeps the decision engine fully generic — see
  `ARCHITECTURE.md` for why that mattered more than getting bespoke per-market weighting "right".

## What's intentionally not here

Per the task brief: no real eID/KYC/registry/credit-bureau/banking integrations, no production
authentication, no cloud deployment, no pixel-perfect UI, no legally definitive compliance logic.

## What I'd do next

- Make beneficial owners a repeatable sub-form (data model already supports it via JSON step data;
  needs a small UI/validation change, not a flow-engine change).
- Add optimistic locking on `OnboardingApplicationEntity` — two tabs resuming the same link
  concurrently could race today.
- Move mock "attempt" tracking from a repository count to an explicit counter field, so retries
  survive out-of-order or concurrent requests more predictably.
- Swap H2 for Postgres and add Flyway migrations before this went anywhere near production data.
