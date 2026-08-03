# Architecture & flow diagrams

All diagrams are Mermaid, rendered natively by GitHub. Each file is self-contained and can be
read on its own; together they go from "what talks to what" down to "what happens on one click."

| # | File | What it answers |
|---|---|---|
| 1 | [System architecture](diagrams/01-system-architecture.md) | What are the layers/packages and how do they depend on each other? |
| 2 | [Flow-engine class diagram](diagrams/02-flow-engine-class-diagram.md) | How is "one flow per market" modelled so adding a market needs no new branching code? |
| 3 | [Integration & decisioning class diagram](diagrams/03-integration-decisioning-class-diagram.md) | How do the five mock external checks plug in, and how does a decision get made from their results? |
| 4 | [Data model (ERD)](diagrams/04-data-model-erd.md) | What's actually persisted, and how do the tables relate? |
| 5 | [Sequence — start & submit a step](diagrams/05-sequence-start-and-submit-step.md) | Click by click, what happens when someone starts an application and submits a step? |
| 6 | [Sequence — decision, resume, retry](diagrams/06-sequence-decision-resume-retry.md) | What happens at final submission, when someone resumes a dropped session, and when a check fails transiently? |
| 7 | [Application state machine](diagrams/07-application-state-machine.md) | What states can an application be in, and what moves it between them? |
| 8 | [Onboarding flow shape](diagrams/08-onboarding-flow-shape.md) | What does the step-by-step journey look like for a private vs. a business applicant, across markets? |

## How to read these if you're new to the codebase

Start at #1 for the shape of the system, then #8 for what the user actually experiences, then #5
and #6 for how a request turns into a decision under the hood. #2–#4 are reference material for
when you're about to extend something (a new market, a new check, a new persisted field).
