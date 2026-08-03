# Flow-engine class diagram

The core "no if/else tree" mechanism: six market-specific provider beans, one registry, and a
generic `FlowDefinition` / `StepDefinition` / `FieldDefinition` structure that the web layer and
validator both read at runtime instead of having market logic baked into code.

```mermaid
classDiagram
    class FlowDefinitionProvider {
        <<interface>>
        +define() FlowDefinition
    }

    class SwedenPrivateFlowProvider
    class SpainPrivateFlowProvider
    class PolandPrivateFlowProvider
    class SwedenBusinessFlowProvider
    class SpainBusinessFlowProvider
    class PolandBusinessFlowProvider

    FlowDefinitionProvider <|.. SwedenPrivateFlowProvider
    FlowDefinitionProvider <|.. SpainPrivateFlowProvider
    FlowDefinitionProvider <|.. PolandPrivateFlowProvider
    FlowDefinitionProvider <|.. SwedenBusinessFlowProvider
    FlowDefinitionProvider <|.. SpainBusinessFlowProvider
    FlowDefinitionProvider <|.. PolandBusinessFlowProvider

    class FlowRegistry {
        -Map~FlowKey, FlowDefinition~ flows
        +FlowRegistry(List~FlowDefinitionProvider~)
        +get(Country, CustomerType) FlowDefinition
        +find(Country, CustomerType) Optional~FlowDefinition~
        +all() List~FlowDefinition~
    }

    class FlowKey {
        <<record>>
        +Country country
        +CustomerType customerType
    }

    class FlowDefinition {
        <<record>>
        +Country country
        +CustomerType customerType
        +List~StepDefinition~ steps
        +step(key) Optional~StepDefinition~
        +indexOf(key) int
        +firstStep() Optional~StepDefinition~
        +nextStep(key) Optional~StepDefinition~
        +isLastStep(key) boolean
    }

    class StepDefinition {
        <<record>>
        +String key
        +String title
        +String description
        +List~FieldDefinition~ fields
        +IntegrationType integrationType
        +boolean reviewStep
        +integration() Optional~IntegrationType~
    }

    class FieldDefinition {
        <<record>>
        +String name
        +String label
        +FieldType type
        +boolean required
        +String pattern
        +String patternErrorMessage
        +List~String~ options
        +String helpText
    }

    class FieldValidationService {
        +validate(fields, submitted) Map~String, String~
    }

    FlowRegistry o-- "1..*" FlowDefinitionProvider : collects at startup
    FlowRegistry --> FlowKey : keyed by
    FlowRegistry o-- "6" FlowDefinition
    FlowDefinition *-- "1..*" StepDefinition
    StepDefinition *-- "0..*" FieldDefinition
    FieldValidationService ..> FieldDefinition : reads metadata from
```

## Why this shape

- **Open/closed in practice**: adding a 7th flow (e.g. a new country, or a third customer type)
  is one new `@Component` implementing `FlowDefinitionProvider`. `FlowRegistry`, the web layer,
  and `FieldValidationService` need zero changes.
- **`FieldDefinition` is the single source of truth for a form field** — its label, type,
  required-ness, and regex pattern drive both the generic Thymeleaf template (`step.html`) and
  `FieldValidationService`. There is no second place a field's rules could drift out of sync.
- **`StepDefinition.integrationType` is optional** — a step is either pure data collection
  (e.g. contact details) or a data-collection-then-check step (e.g. identity verification). The
  orchestration service branches on *presence* of this field, not on which check it is.
