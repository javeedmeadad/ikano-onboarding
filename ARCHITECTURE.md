# Architecture note

For diagrams (system architecture, class/UML, ERD, sequence diagrams, state machine, flow shape)
see **[docs/DIAGRAMS.md](docs/DIAGRAMS.md)**. This note is the prose companion.

## Goal

The brief's explicit "good senior signal" is: adding a country, a customer type, or a
verification step should not require rewriting the app or growing an if/else tree. Every design
decision below is in service of that.

## Packages

```
domain/        enums shared everywhere (Country, CustomerType, FieldType, IntegrationOutcome, ...)
flow/          the flow engine: field/step/flow definitions + the registry that looks them up
flow/provider/ one class per market (SwedenPrivateFlowProvider, ...) — the only place that
               "knows" what SE/ES/PL or private/business specifically require
entity/        JPA entities (persistence shape)
repository/    Spring Data repositories
integration/   typed request/response + one deterministic mock client per external system
decision/      the single decisioning rule that turns integration outcomes into a verdict
audit/         audit trail writer/reader
validation/    generic, config-driven form validation
service/       OnboardingService — orchestrates a step submission into a flow transition
web/           controllers (thin — no business logic)
```

## The flow engine: config, not code

A step is a `StepDefinition`: a key, a title/description, a list of `FieldDefinition`s, and an
optional `IntegrationType` it triggers on submit. A flow is an ordered list of steps for one
`(Country, CustomerType)` pair. Six `@Component` classes — one per market — each implement
`FlowDefinitionProvider` and return their `FlowDefinition`. `FlowRegistry` collects every
provider bean at startup into a `Map<FlowKey, FlowDefinition>`.

**Adding a seventh market (say, Sweden sole-trader as a third customer type) means writing one
new provider class and nothing else.** No controller, service method, template, or validator
changes — the web layer, `OnboardingService`, and `FieldValidationService` are all driven purely
by the `FlowDefinition` they're handed at runtime. That's the concrete answer to "avoid a large
nested if/else tree."

The web layer follows the same principle one level down: `step.html` is one Thymeleaf template
that renders whatever fields the current `StepDefinition` declares (text/number/select/
checkbox/textarea), and `FieldValidationService` validates required-ness, regex patterns, and
numeric-ness purely from that same metadata. Adding a field to one market's step is a one-line
change in that market's provider — the rendering and validation code never changes.

## Data model

- `OnboardingApplicationEntity` — one row per application: country, customer type, current step,
  status, resume token + expiry, final decision + reason, timestamps.
- `StepRecordEntity` — one row per (application, step): submitted data as JSON, a content hash
  (used for resumability, see below), status, completion time.
- `IntegrationResultEntity` — one row per mock check actually performed: type, request ID,
  normalized outcome, service-specific detail code, human summary, timestamp.
- `AuditEntryEntity` — one row per meaningful event (step started/completed, check performed,
  decision made). Deliberately stores only outcome codes and step keys — never the underlying
  answers — so it's safe to show to a support agent without a data-handling review.

## Integration layer

Every mock implements `IntegrationClient` and returns an `IntegrationResponse` with a normalized
`IntegrationOutcome` (`SUCCESS` / `MANUAL_REVIEW` / `FAIL`) plus a service-specific `detailCode`
for the audit trail, and a `retryable` flag for transient failures. Normalizing at the client
boundary is what lets `DecisionEngine` stay a single ~20-line rule instead of growing a branch per
integration type or per market — it never sees "expired_id" vs. "dissolved" vs. "confirmed_hit",
only SUCCESS/MANUAL_REVIEW/FAIL. Swapping a mock for a real HTTP client later means implementing
one interface; nothing upstream changes.

`BankAccountMockClient` demonstrates handling a flaky dependency: an IBAN ending `0000` times out
on the first attempt (`retryable=true`) and succeeds on the second, driven by an `attempt` count
the service layer derives from prior `IntegrationResultEntity` rows for that application+type.

## Decisioning

`DecisionEngine.decide(List<IntegrationResultEntity>)` applies one precedence rule — any FAIL
rejects, else any MANUAL_REVIEW refers, else approve — across the *latest* result per integration
type for the application (so a corrected retry supersedes an earlier failure). It has no
knowledge of country or customer type; it only sees normalized outcomes. This is the same
open/closed idea as the flow engine, applied to decisioning.

## Orchestration (`OnboardingService`)

`submitStep` is the one place a step submission becomes a flow transition:

1. Validate the posted fields against the step's `FieldDefinition`s.
2. If valid and the step has an integration, aggregate every field collected so far in the
   application (not just this step — e.g. the credit step reuses income/debt captured earlier),
   call the mock client, and persist the result.
3. On FAIL, stay on the same step and surface the failure (with a retry hint if transient).
4. On SUCCESS/MANUAL_REVIEW, mark the step complete and advance `currentStepKey` to
   `flow.nextStep(...)`.
5. On the review step's submission, aggregate every integration result recorded for the
   application, run `DecisionEngine`, and persist the final decision + status.

Every transition writes an audit entry with a fresh request ID.

## Resumability

- A resume token (opaque UUID) and 24h expiry are generated when an application starts — never
  the raw application ID, per the brief's explicit guidance.
- Visiting an expired resume link lazily flips the application to `EXPIRED` rather than requiring
  a background sweep, keeping the demo self-contained.
- Resubmitting a step whose data hasn't changed (detected via a SHA-256 hash of the submitted
  field map) skips re-running its integration call — the bonus requirement to "avoid re-running
  expensive or sensitive checks unless the input changed" — while still being idempotent for a
  user who just clicks back and forward.
- `ApplicationStatus` distinguishes `IN_PROGRESS`, `SUBMITTED`, `APPROVED`, `MANUAL_REVIEW`,
  `REJECTED`, `EXPIRED`, and `ABANDONED` so a resumed session, a finished one, and a dead one are
  never conflated.

## Tradeoffs made under the ~6-8h timebox

See `README.md#assumptions-and-deliberate-simplifications` for the specific list (combined
sub-checks, single-UBO model, no abandonment sweep, etc.) — each is a scope cut chosen to keep the
flow engine, decisioning, and resumability mechanisms fully real rather than spreading effort
thin across every field a production onboarding form would eventually need.
