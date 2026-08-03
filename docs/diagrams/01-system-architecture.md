# System architecture

Layered dependency view. Arrows point from a layer to what it depends on. Nothing in `web`,
`service`, or `validation` depends on `domain.Country`/`domain.CustomerType` branching logic —
market-specific behaviour only lives in `flow.provider`.

```mermaid
flowchart TB
    subgraph Client
        Browser["Browser<br/>(Thymeleaf-rendered forms)"]
    end

    subgraph Web["web — controllers (thin, no business logic)"]
        Home[HomeController]
        Onboard[OnboardingController]
        Resume[ResumeController]
    end

    subgraph Core["service — orchestration"]
        Svc[OnboardingService]
    end

    subgraph Support["supporting layers"]
        Flow["flow — FlowRegistry<br/>+ 6 FlowDefinitionProvider beans"]
        Valid["validation — FieldValidationService"]
        Integ["integration — IntegrationClientRegistry<br/>+ 5 mock clients"]
        Dec["decision — DecisionEngine"]
        Audit["audit — AuditService"]
    end

    subgraph Persistence["entity + repository — Spring Data JPA"]
        Repo[(H2 / PostgreSQL)]
    end

    Browser -->|HTTP form POST/GET| Web
    Home --> Svc
    Onboard --> Svc
    Resume --> Svc

    Svc --> Flow
    Svc --> Valid
    Svc --> Integ
    Svc --> Dec
    Svc --> Audit
    Svc --> Repo

    Flow -.->|"defines fields & steps for"| Valid
    Integ -.->|"outcomes feed"| Dec
    Audit --> Repo

    classDef layer fill:#fef2f2,stroke:#e2001a,stroke-width:1px;
    class Web,Core,Support,Persistence layer;
```

## Why this shape

- **`OnboardingService` is the only class that knows how a step submission becomes a flow
  transition.** Everything else (validation, integration calls, decisioning, audit) is a pure
  function it calls — none of them hold flow-transition logic themselves.
- **`FlowRegistry` is built once, from Spring-discovered beans**, not a switch statement — see
  [diagram 2](02-flow-engine-class-diagram.md).
- **Controllers never touch a repository or a domain rule directly.** They translate HTTP
  in and out; `OnboardingService` is the single seam a test (or a future non-web client, e.g. an
  API) would call through.
