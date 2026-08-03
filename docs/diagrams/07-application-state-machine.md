# Application state machine

`ApplicationStatus` distinguishes an in-progress session from every kind of "done" so a resumed
session, a finished one, and a dead one are never conflated.

```mermaid
stateDiagram-v2
    [*] --> IN_PROGRESS : start(country, customerType)

    IN_PROGRESS --> IN_PROGRESS : submit a non-review step<br/>(SUCCESS or MANUAL_REVIEW outcome)
    IN_PROGRESS --> SUBMITTED : submit REVIEW_SUBMIT step
    SUBMITTED --> APPROVED : DecisionEngine → no FAIL, no MANUAL_REVIEW
    SUBMITTED --> MANUAL_REVIEW : DecisionEngine → no FAIL, ≥1 MANUAL_REVIEW
    SUBMITTED --> REJECTED : DecisionEngine → ≥1 FAIL

    IN_PROGRESS --> EXPIRED : resume link visited after\nresumeTokenExpiresAt has passed

    IN_PROGRESS --> ABANDONED : (not currently triggered —\nwould need a scheduled sweep;\nsee README "what I'd do next")

    APPROVED --> [*]
    MANUAL_REVIEW --> [*]
    REJECTED --> [*]
    EXPIRED --> [*]
    ABANDONED --> [*]

    note right of IN_PROGRESS
        A step submission that fails an
        integration check (FAIL outcome)
        does NOT change application status —
        it stays IN_PROGRESS on the same step
        until the applicant corrects and resubmits.
    end note

    note right of SUBMITTED
        SUBMITTED is set the instant the review
        step is accepted, then immediately resolved
        to a terminal decision status in the same
        request — it is not a separately visited
        waiting state today, but exists so a
        future async decisioning step has
        somewhere to sit.
    end note
```

## Why this shape

- **`EXPIRED` vs `ABANDONED`** are deliberately separate states even though only `EXPIRED` is
  currently reachable: `EXPIRED` means "the resume window ran out", `ABANDONED` would mean "no
  activity for N days regardless of token expiry" — a genuinely different signal for reporting,
  worth keeping distinct even though only one is wired up in this take-home scope.
- **A per-step integration `FAIL` is not a status transition.** It's local to the step
  (`StepRecordEntity.status = FAILED`); the application-level status only ever reflects where the
  applicant is in the overall journey, not the outcome of their last keystroke.
