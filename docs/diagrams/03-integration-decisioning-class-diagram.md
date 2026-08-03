# Integration & decisioning class diagram

Five deterministic mock clients behind one interface, normalized to a tri-state outcome, so the
decision engine never has to know which specific check produced its input.

```mermaid
classDiagram
    class IntegrationClient {
        <<interface>>
        +type() IntegrationType
        +call(IntegrationRequest) IntegrationResponse
    }

    class IdentityMockClient
    class KybRegistryMockClient
    class PepSanctionsMockClient
    class CreditAffordabilityMockClient
    class BankAccountMockClient

    IntegrationClient <|.. IdentityMockClient
    IntegrationClient <|.. KybRegistryMockClient
    IntegrationClient <|.. PepSanctionsMockClient
    IntegrationClient <|.. CreditAffordabilityMockClient
    IntegrationClient <|.. BankAccountMockClient

    class IntegrationClientRegistry {
        -Map~IntegrationType, IntegrationClient~ clients
        +call(IntegrationType, IntegrationRequest) IntegrationResponse
    }

    class IntegrationRequest {
        <<record>>
        +String requestId
        +Country country
        +CustomerType customerType
        +Map~String, String~ data
        +int attempt
        +field(name) String
    }

    class IntegrationResponse {
        <<record>>
        +IntegrationOutcome outcome
        +String detailCode
        +String summary
        +boolean retryable
        +success(detail, summary)$ IntegrationResponse
        +manualReview(detail, summary)$ IntegrationResponse
        +fail(detail, summary)$ IntegrationResponse
        +retryableFail(detail, summary)$ IntegrationResponse
    }

    class IntegrationOutcome {
        <<enumeration>>
        SUCCESS
        MANUAL_REVIEW
        FAIL
    }

    IntegrationClientRegistry o-- "5" IntegrationClient
    IntegrationClient ..> IntegrationRequest : consumes
    IntegrationClient ..> IntegrationResponse : produces
    IntegrationResponse --> IntegrationOutcome

    class DecisionEngine {
        +decide(List~IntegrationResultEntity~) DecisionResult
    }

    class DecisionResult {
        <<record>>
        +DecisionOutcome outcome
        +String reason
    }

    class DecisionOutcome {
        <<enumeration>>
        APPROVED
        MANUAL_REVIEW
        REJECTED
    }

    class IntegrationResultEntity {
        +UUID id
        +UUID applicationId
        +IntegrationType integrationType
        +String requestId
        +IntegrationOutcome outcome
        +String detailCode
        +String summary
        +Instant checkedAt
    }

    DecisionEngine ..> IntegrationResultEntity : reads latest-per-type
    DecisionEngine --> DecisionResult
    DecisionResult --> DecisionOutcome
    IntegrationClientRegistry ..> IntegrationResultEntity : OnboardingService persists response as
```

## Decisioning rule (deliberately the whole rule)

```mermaid
flowchart LR
    A["Take latest result per\nIntegrationType for the application"] --> B{"Any FAIL?"}
    B -- yes --> C["REJECTED"]
    B -- no --> D{"Any MANUAL_REVIEW?"}
    D -- yes --> E["MANUAL_REVIEW"]
    D -- no --> F["APPROVED"]
```

## Why this shape

- **Normalization happens at the client boundary.** `expired_id`, `dissolved`, and
  `confirmed_hit` are all just `FAIL` by the time `DecisionEngine` sees them — the specific
  `detailCode` is kept only for the audit trail and the UI message, never branched on again.
- **`DecisionEngine` has no knowledge of `Country` or `CustomerType`.** The same ~20-line rule
  handles all six flows. Precedence is FAIL > MANUAL_REVIEW > APPROVED, applied to the latest
  result per integration type (so a corrected retry supersedes an earlier failure).
- **Swapping a mock for a real client** (e.g. a real credit bureau API) means implementing
  `IntegrationClient` once — `IntegrationClientRegistry`, `OnboardingService`, and
  `DecisionEngine` are unaffected.
