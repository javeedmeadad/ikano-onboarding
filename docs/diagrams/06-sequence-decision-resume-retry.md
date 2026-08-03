# Sequence — final decision, resuming, and a transient retry

## Submitting the review step → final decision

```mermaid
sequenceDiagram
    actor User
    participant Web as OnboardingController
    participant Svc as OnboardingService
    participant DB as Repositories (H2)
    participant Dec as DecisionEngine
    participant Audit as AuditService

    User->>Web: POST /onboarding/{id}/step<br/>(stepKey=REVIEW_SUBMIT, acceptTerms=true)
    Web->>Svc: submitStep(id, "REVIEW_SUBMIT", formData)
    Svc->>DB: save StepRecord (COMPLETED)
    Svc->>Svc: app.status = SUBMITTED
    Svc->>DB: load all IntegrationResults for application
    Svc->>Svc: keep only latest result per IntegrationType
    Svc->>Dec: decide(latestResultsPerType)
    Dec-->>Svc: DecisionResult(outcome, reason)
    Svc->>Svc: app.finalDecision = outcome<br/>app.status = APPROVED / MANUAL_REVIEW / REJECTED
    Svc->>DB: save application
    Svc->>Audit: log(APPLICATION_SUBMITTED)
    Svc->>Audit: log(DECISION_MADE, outcome + reason)
    Svc-->>Web: StepSubmissionOutcome.completed()
    Web-->>User: 302 redirect to /onboarding/{id}/result
    User->>Web: GET /onboarding/{id}/result
    Web->>Svc: summary(id)
    Svc->>DB: load application, steps, integration results, audit trail
    Svc-->>Web: ApplicationSummary
    Web-->>User: decision, reason, checks performed, audit trail, submitted answers
```

## Resuming a dropped-off application

```mermaid
sequenceDiagram
    actor User
    participant Web as ResumeController
    participant Svc as OnboardingService
    participant DB as Repositories (H2)
    participant Audit as AuditService

    User->>Web: GET /resume/{token}
    Web->>Svc: resolveResumeToken(token)
    Svc->>DB: findByResumeToken(token)

    alt token not found
        DB-->>Svc: empty
        Svc-->>Web: Optional.empty()
        Web-->>User: "This link is not recognised"
    else token found, but expired and still IN_PROGRESS
        DB-->>Svc: application
        Svc->>Svc: expiresAt is in the past
        Svc->>Svc: app.status = EXPIRED
        Svc->>DB: save application
        Svc->>Audit: log(APPLICATION_EXPIRED)
        Svc-->>Web: application (status=EXPIRED)
        Web-->>User: "This resume link has expired"
    else token found, still valid, IN_PROGRESS
        DB-->>Svc: application
        Svc-->>Web: application (status=IN_PROGRESS)
        Web-->>User: 302 redirect to /onboarding/{id}/step<br/>(lands on currentStepKey, exactly where they left off)
    else token found, application already finished
        DB-->>Svc: application (SUBMITTED/APPROVED/MANUAL_REVIEW/REJECTED)
        Svc-->>Web: application
        Web-->>User: 302 redirect to /onboarding/{id}/result
    end
```

## Transient failure → retry (bank account verification)

```mermaid
sequenceDiagram
    actor User
    participant Svc as OnboardingService
    participant Integ as IntegrationClientRegistry
    participant Bank as BankAccountMockClient
    participant DB as Repositories (H2)

    Note over User,Bank: IBAN ends in "0000" — simulates a flaky downstream dependency

    User->>Svc: submit BANK_ACCOUNT_VERIFICATION (attempt 1)
    Svc->>DB: count prior IntegrationResults for this type → 0, so attempt=1
    Svc->>Integ: call(BANK_ACCOUNT, request{attempt=1})
    Integ->>Bank: call(request)
    Bank-->>Integ: FAIL("unreachable", retryable=true)
    Integ-->>Svc: FAIL, retryable
    Svc->>DB: save IntegrationResult (FAIL)
    Svc-->>User: 200, error banner: "Temporary issue — please retry"

    User->>Svc: resubmit BANK_ACCOUNT_VERIFICATION, same IBAN (attempt 2)
    Svc->>DB: count prior IntegrationResults for this type → 1, so attempt=2
    Svc->>Integ: call(BANK_ACCOUNT, request{attempt=2})
    Integ->>Bank: call(request)
    Bank-->>Integ: SUCCESS("iban_verified")
    Integ-->>Svc: SUCCESS
    Svc->>DB: save IntegrationResult (SUCCESS)<br/>save StepRecord (COMPLETED)
    Svc-->>User: 302 redirect to next step
```

## Why this shape

- **The decision is computed once, synchronously, at review submission** — not per-step — so a
  step's individual outcome (e.g. a manual-review PEP hit) never prematurely ends the journey;
  only the aggregate at the end does.
- **Resume tokens are checked lazily.** There's no background sweep marking applications expired
  on a timer; the check happens the moment someone actually uses the link, which is the only
  moment it matters for a UX standpoint and keeps the demo self-contained (documented as a
  known simplification in the README).
- **`attempt` is derived from existing `IntegrationResult` rows**, not a separate counter field —
  simple, but as noted in the README this means concurrent retries from two tabs could race; an
  explicit counter would be the production fix.
