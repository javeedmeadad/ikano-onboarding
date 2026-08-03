# Application state machine

`ApplicationStatus` distinguishes an in-progress session from every kind of "done" so a resumed
session, a finished one, and a dead one are never conflated.

```mermaid
stateDiagram-v2
    [*] --> IN_PROGRESS : start application

    IN_PROGRESS --> IN_PROGRESS : submit a non-review step, success or manual review
    IN_PROGRESS --> SUBMITTED : submit review step
    SUBMITTED --> APPROVED : decision, no fail, no manual review
    SUBMITTED --> MANUAL_REVIEW : decision, no fail, at least one manual review
    SUBMITTED --> REJECTED : decision, at least one fail

    IN_PROGRESS --> EXPIRED : resume link visited after expiry
    IN_PROGRESS --> ABANDONED : not currently triggered, would need a scheduled sweep

    APPROVED --> [*]
    MANUAL_REVIEW --> [*]
    REJECTED --> [*]
    EXPIRED --> [*]
    ABANDONED --> [*]

    note right of IN_PROGRESS
        A step submission that fails an integration check
        does not change application status. It stays
        IN_PROGRESS on the same step until the applicant
        corrects and resubmits.
    end note

    note right of SUBMITTED
        SUBMITTED is set the instant the review step is
        accepted, then immediately resolved to a terminal
        decision status in the same request. It is not a
        separately visited waiting state today, but exists
        so a future async decisioning step has somewhere
        to sit.
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
