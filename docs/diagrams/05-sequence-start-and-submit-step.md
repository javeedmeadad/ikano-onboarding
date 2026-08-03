# Sequence — start an application & submit a step

## Starting an application

```mermaid
sequenceDiagram
    actor User
    participant Web as OnboardingController
    participant Svc as OnboardingService
    participant Flow as FlowRegistry
    participant DB as Repositories (H2)
    participant Audit as AuditService

    User->>Web: POST /onboarding/start<br/>(country, customerType)
    Web->>Svc: start(country, customerType)
    Svc->>Flow: get(country, customerType)
    Flow-->>Svc: FlowDefinition (6 pre-registered)
    Svc->>Svc: build OnboardingApplicationEntity<br/>(status=IN_PROGRESS, currentStepKey=firstStep,<br/>resumeToken=UUID, expiresAt=now+24h)
    Svc->>DB: save application
    Svc->>Audit: log(APPLICATION_STARTED)
    Audit->>DB: save audit entry
    Svc-->>Web: OnboardingApplicationEntity
    Web-->>User: 302 redirect to /onboarding/{id}/step
```

## Submitting a step (three outcomes)

```mermaid
sequenceDiagram
    actor User
    participant Web as OnboardingController
    participant Svc as OnboardingService
    participant Valid as FieldValidationService
    participant Flow as FlowRegistry
    participant Integ as IntegrationClientRegistry
    participant Mock as (matching) MockClient
    participant DB as Repositories (H2)
    participant Audit as AuditService

    User->>Web: POST /onboarding/{id}/step<br/>(stepKey + form fields)
    Web->>Svc: submitStep(id, stepKey, formData)
    Svc->>Flow: get(app.country, app.customerType)
    Flow-->>Svc: FlowDefinition
    Svc->>Valid: validate(step.fields(), formData)

    alt required field missing or pattern mismatch
        Valid-->>Svc: fieldErrors (non-empty)
        Svc-->>Web: StepSubmissionOutcome.invalid(errors)
        Web-->>User: 200, same step re-rendered<br/>with inline errors + entered values
    else fields valid, step has no integration
        Valid-->>Svc: no errors
        Svc->>DB: save StepRecord (COMPLETED)
        Svc->>Audit: log(STEP_COMPLETED)
        Svc->>DB: advance currentStepKey to flow.nextStep(stepKey)
        Svc-->>Web: StepSubmissionOutcome.advanced()
        Web-->>User: 302 redirect to next step
    else fields valid, step has an integration (e.g. identity check)
        Valid-->>Svc: no errors
        Svc->>DB: aggregate all prior step data for this application
        Svc->>Integ: call(type, IntegrationRequest{aggregatedData, attempt})
        Integ->>Mock: call(request)
        Mock-->>Integ: IntegrationResponse(outcome, detailCode, retryable)
        Integ-->>Svc: IntegrationResponse
        Svc->>DB: save IntegrationResult
        Svc->>Audit: log(INTEGRATION_CHECK, outcome+detailCode)

        alt outcome == FAIL
            Svc->>DB: save StepRecord (FAILED)
            Svc-->>Web: StepSubmissionOutcome.integrationFailure(summary, retryable)
            Web-->>User: 200, same step re-rendered<br/>with error banner (retry hint if transient)
        else outcome == SUCCESS or MANUAL_REVIEW
            Svc->>DB: save StepRecord (COMPLETED)
            Svc->>Audit: log(STEP_COMPLETED)
            Svc->>DB: advance currentStepKey
            Svc-->>Web: StepSubmissionOutcome.advanced()
            Web-->>User: 302 redirect to next step<br/>(MANUAL_REVIEW does not block progress)
        end
    end
```

## Why this shape

- **Validation always runs before any integration is called** — an invalid form never triggers a
  (mock, but conceptually billable/rate-limited) external check.
- **A FAIL response never advances the flow**, but the user isn't stuck: their entered values are
  preserved and re-rendered, so correcting one field (e.g. an ID number) and resubmitting is the
  entire recovery path — no separate "go back" affordance needed.
- **MANUAL_REVIEW never blocks** — it's recorded and only surfaces at the final decision, matching
  how a real onboarding product keeps the applicant moving while a human reviews in parallel.
