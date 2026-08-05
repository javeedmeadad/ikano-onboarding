package com.ikano.onboarding.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ikano.onboarding.audit.AuditService;
import com.ikano.onboarding.decision.DecisionEngine;
import com.ikano.onboarding.domain.ApplicationStatus;
import com.ikano.onboarding.domain.Country;
import com.ikano.onboarding.domain.CustomerType;
import com.ikano.onboarding.domain.IntegrationOutcome;
import com.ikano.onboarding.domain.IntegrationType;
import com.ikano.onboarding.domain.StepStatus;
import com.ikano.onboarding.entity.IntegrationResultEntity;
import com.ikano.onboarding.entity.OnboardingApplicationEntity;
import com.ikano.onboarding.entity.StepRecordEntity;
import com.ikano.onboarding.flow.FlowDefinition;
import com.ikano.onboarding.flow.FlowRegistry;
import com.ikano.onboarding.flow.StepDefinition;
import com.ikano.onboarding.integration.IntegrationClientRegistry;
import com.ikano.onboarding.integration.IntegrationRequest;
import com.ikano.onboarding.integration.IntegrationResponse;
import com.ikano.onboarding.repository.IntegrationResultRepository;
import com.ikano.onboarding.repository.OnboardingApplicationRepository;
import com.ikano.onboarding.repository.StepRecordRepository;
import com.ikano.onboarding.service.dto.ApplicationSummary;
import com.ikano.onboarding.service.dto.StepPageModel;
import com.ikano.onboarding.service.dto.StepSubmissionOutcome;
import com.ikano.onboarding.validation.FieldValidationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Orchestrates the whole onboarding journey: create an application, render/validate one step at
 * a time, call the right mock integration, aggregate outcomes into a decision, and support
 * resuming a dropped application. This is the one place that "knows" how a step submission turns
 * into a flow transition — everything market-specific comes from {@link FlowRegistry}.
 */
@Service
public class OnboardingService {

    private static final Duration RESUME_TOKEN_TTL = Duration.ofHours(24);

    private final OnboardingApplicationRepository applicationRepository;
    private final StepRecordRepository stepRecordRepository;
    private final IntegrationResultRepository integrationResultRepository;
    private final FlowRegistry flowRegistry;
    private final FieldValidationService validationService;
    private final IntegrationClientRegistry integrationClientRegistry;
    private final DecisionEngine decisionEngine;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public OnboardingService(OnboardingApplicationRepository applicationRepository,
                              StepRecordRepository stepRecordRepository,
                              IntegrationResultRepository integrationResultRepository,
                              FlowRegistry flowRegistry,
                              FieldValidationService validationService,
                              IntegrationClientRegistry integrationClientRegistry,
                              DecisionEngine decisionEngine,
                              AuditService auditService,
                              ObjectMapper objectMapper) {
        this.applicationRepository = applicationRepository;
        this.stepRecordRepository = stepRecordRepository;
        this.integrationResultRepository = integrationResultRepository;
        this.flowRegistry = flowRegistry;
        this.validationService = validationService;
        this.integrationClientRegistry = integrationClientRegistry;
        this.decisionEngine = decisionEngine;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public OnboardingApplicationEntity start(Country country, CustomerType customerType) {
        FlowDefinition flow = flowRegistry.get(country, customerType);
        StepDefinition first = flow.firstStep().orElseThrow();

        OnboardingApplicationEntity app = new OnboardingApplicationEntity();
        app.setCountry(country);
        app.setCustomerType(customerType);
        app.setCurrentStepKey(first.key());
        app.setResumeToken(UUID.randomUUID().toString());
        app.setResumeTokenExpiresAt(Instant.now().plus(RESUME_TOKEN_TTL));
        applicationRepository.save(app);

        auditService.log(app.getId(), newRequestId(), "APPLICATION_STARTED",
                "Application started for " + country + " / " + customerType);
        return app;
    }

    public OnboardingApplicationEntity get(UUID id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Application not found: " + id));
    }

    @Transactional
    public Optional<OnboardingApplicationEntity> resolveResumeToken(String token) {
        return applicationRepository.findByResumeToken(token).map(this::expireIfNeeded);
    }

    private OnboardingApplicationEntity expireIfNeeded(OnboardingApplicationEntity app) {
        if (app.getStatus() == ApplicationStatus.IN_PROGRESS && app.getResumeTokenExpiresAt().isBefore(Instant.now())) {
            app.setStatus(ApplicationStatus.EXPIRED);
            app.setUpdatedAt(Instant.now());
            applicationRepository.save(app);
            auditService.log(app.getId(), newRequestId(), "APPLICATION_EXPIRED",
                    "Resume link expired; application marked expired.");
        }
        return app;
    }

    public StepPageModel loadStep(UUID applicationId, String requestedStepKey) {
        OnboardingApplicationEntity app = get(applicationId);
        FlowDefinition flow = flowRegistry.get(app.getCountry(), app.getCustomerType());
        String stepKey = requestedStepKey != null ? requestedStepKey : app.getCurrentStepKey();
        StepDefinition step = flow.step(stepKey)
                .orElseThrow(() -> new NoSuchElementException("Unknown step: " + stepKey));

        Map<String, String> previousValues = stepRecordRepository.findByApplicationIdAndStepKey(applicationId, stepKey)
                .map(r -> readData(r.getDataJson()))
                .orElse(Map.of());

        List<String> completedKeys = stepRecordRepository.findByApplicationIdOrderByCompletedAtAsc(applicationId).stream()
                .filter(r -> r.getStatus() == StepStatus.COMPLETED)
                .map(StepRecordEntity::getStepKey)
                .toList();

        String failureMessage = null;
        boolean retryable = false;
        Optional<StepRecordEntity> record = stepRecordRepository.findByApplicationIdAndStepKey(applicationId, stepKey);
        if (record.isPresent() && record.get().getStatus() == StepStatus.FAILED && step.integration().isPresent()) {
            Optional<IntegrationResultEntity> lastResult = integrationResultRepository
                    .findTopByApplicationIdAndIntegrationTypeOrderByCheckedAtDesc(applicationId, step.integration().get());
            if (lastResult.isPresent() && lastResult.get().getOutcome() == IntegrationOutcome.FAIL) {
                failureMessage = lastResult.get().getSummary();
                retryable = "unreachable".equals(lastResult.get().getDetailCode());
            }
        }

        Map<String, Map<String, String>> reviewData = step.reviewStep() ? reviewData(applicationId, flow) : Map.of();

        return new StepPageModel(app.getId(), app.getStatus(), app.getCountry(), app.getCustomerType(),
                flow.steps(), completedKeys, flow.indexOf(stepKey), step, previousValues, Map.of(),
                failureMessage, retryable, "/#/resume/" + app.getResumeToken(), reviewData);
    }

    private Map<String, Map<String, String>> reviewData(UUID applicationId, FlowDefinition flow) {
        Map<String, Map<String, String>> data = new LinkedHashMap<>();
        for (StepRecordEntity r : stepRecordRepository.findByApplicationIdOrderByCompletedAtAsc(applicationId)) {
            if (r.getStatus() != StepStatus.COMPLETED) {
                continue;
            }
            flow.step(r.getStepKey()).ifPresent(def -> {
                if (!def.reviewStep()) {
                    data.put(def.title(), readData(r.getDataJson()));
                }
            });
        }
        return data;
    }

    @Transactional
    public StepSubmissionOutcome submitStep(UUID applicationId, String stepKey, Map<String, String> formData) {
        OnboardingApplicationEntity app = get(applicationId);
        if (app.getStatus() != ApplicationStatus.IN_PROGRESS) {
            throw new IllegalStateException("Application " + applicationId + " is not in progress (status=" + app.getStatus() + ")");
        }

        FlowDefinition flow = flowRegistry.get(app.getCountry(), app.getCustomerType());
        StepDefinition step = flow.step(stepKey)
                .orElseThrow(() -> new NoSuchElementException("Unknown step: " + stepKey));

        Map<String, String> errors = validationService.validate(step.fields(), formData);
        if (!errors.isEmpty()) {
            return StepSubmissionOutcome.invalid(errors);
        }

        String dataJson = writeData(formData);
        String dataHash = hash(formData);

        StepRecordEntity record = stepRecordRepository.findByApplicationIdAndStepKey(applicationId, stepKey)
                .orElseGet(StepRecordEntity::new);
        boolean unchangedCompletedResubmit = record.getId() != null
                && record.getStatus() == StepStatus.COMPLETED
                && dataHash.equals(record.getDataHash());

        record.setApplicationId(applicationId);
        record.setStepKey(stepKey);
        record.setDataJson(dataJson);
        record.setDataHash(dataHash);

        if (step.integration().isPresent() && !unchangedCompletedResubmit) {
            IntegrationType type = step.integration().get();
            Map<String, String> aggregated = aggregatedData(applicationId);
            aggregated.putAll(formData);

            int attempt = countPriorAttempts(applicationId, type) + 1;
            String requestId = newRequestId();
            IntegrationResponse response = integrationClientRegistry.call(type,
                    new IntegrationRequest(requestId, app.getCountry(), app.getCustomerType(), aggregated, attempt));

            IntegrationResultEntity resultEntity = new IntegrationResultEntity();
            resultEntity.setApplicationId(applicationId);
            resultEntity.setIntegrationType(type);
            resultEntity.setRequestId(requestId);
            resultEntity.setOutcome(response.outcome());
            resultEntity.setDetailCode(response.detailCode());
            resultEntity.setSummary(response.summary());
            integrationResultRepository.save(resultEntity);

            auditService.log(applicationId, requestId, "INTEGRATION_CHECK",
                    type + " check on step " + stepKey + " returned " + response.outcome() + " (" + response.detailCode() + "), attempt " + attempt);

            if (response.outcome() == IntegrationOutcome.FAIL) {
                record.setStatus(StepStatus.FAILED);
                stepRecordRepository.save(record);
                return StepSubmissionOutcome.integrationFailure(response.summary(), response.retryable());
            }
        } else if (unchangedCompletedResubmit) {
            auditService.log(applicationId, newRequestId(), "STEP_RESUBMIT_SKIPPED_CHECK",
                    "Step " + stepKey + " resubmitted with unchanged data; skipped re-running the integration check.");
        }

        record.setStatus(StepStatus.COMPLETED);
        record.setCompletedAt(Instant.now());
        stepRecordRepository.save(record);
        auditService.log(applicationId, newRequestId(), "STEP_COMPLETED", "Step " + stepKey + " completed.");

        if (step.reviewStep()) {
            app.setStatus(ApplicationStatus.SUBMITTED);
            finalizeDecision(app);
            app.setUpdatedAt(Instant.now());
            applicationRepository.save(app);
            auditService.log(applicationId, newRequestId(), "APPLICATION_SUBMITTED", "Application submitted for a final decision.");
            return StepSubmissionOutcome.completed();
        }

        flow.nextStep(stepKey).ifPresent(next -> app.setCurrentStepKey(next.key()));
        app.setUpdatedAt(Instant.now());
        applicationRepository.save(app);
        return StepSubmissionOutcome.advanced();
    }

    private void finalizeDecision(OnboardingApplicationEntity app) {
        List<IntegrationResultEntity> results = integrationResultRepository.findByApplicationIdOrderByCheckedAtAsc(app.getId());

        Map<IntegrationType, IntegrationResultEntity> latestByType = new LinkedHashMap<>();
        for (IntegrationResultEntity r : results) {
            latestByType.put(r.getIntegrationType(), r);
        }

        DecisionEngine.DecisionResult decision = decisionEngine.decide(new ArrayList<>(latestByType.values()));
        app.setFinalDecision(decision.outcome());
        app.setDecisionReason(decision.reason());
        app.setStatus(switch (decision.outcome()) {
            case APPROVED -> ApplicationStatus.APPROVED;
            case MANUAL_REVIEW -> ApplicationStatus.MANUAL_REVIEW;
            case REJECTED -> ApplicationStatus.REJECTED;
        });

        auditService.log(app.getId(), newRequestId(), "DECISION_MADE",
                "Final decision: " + decision.outcome() + " - " + decision.reason());
    }

    public ApplicationSummary summary(UUID applicationId) {
        OnboardingApplicationEntity app = get(applicationId);
        List<StepRecordEntity> steps = stepRecordRepository.findByApplicationIdOrderByCompletedAtAsc(applicationId);
        Map<String, Map<String, String>> stepData = new LinkedHashMap<>();
        for (StepRecordEntity s : steps) {
            stepData.put(s.getStepKey(), readData(s.getDataJson()));
        }
        List<IntegrationResultEntity> integrationResults = integrationResultRepository.findByApplicationIdOrderByCheckedAtAsc(applicationId);
        return new ApplicationSummary(app, steps, stepData, integrationResults, auditService.history(applicationId));
    }

    private Map<String, String> aggregatedData(UUID applicationId) {
        Map<String, String> merged = new LinkedHashMap<>();
        for (StepRecordEntity r : stepRecordRepository.findByApplicationIdOrderByCompletedAtAsc(applicationId)) {
            merged.putAll(readData(r.getDataJson()));
        }
        return merged;
    }

    private int countPriorAttempts(UUID applicationId, IntegrationType type) {
        return (int) integrationResultRepository.findByApplicationIdOrderByCheckedAtAsc(applicationId).stream()
                .filter(r -> r.getIntegrationType() == type)
                .count();
    }

    private Map<String, String> readData(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<HashMap<String, String>>() {
            });
        } catch (Exception e) {
            throw new IllegalStateException("Corrupt step data", e);
        }
    }

    private String writeData(Map<String, String> data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            throw new IllegalStateException("Could not serialize step data", e);
        }
    }

    private String hash(Map<String, String> data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String canonical = new TreeMap<>(data).toString();
            byte[] bytes = digest.digest(canonical.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private String newRequestId() {
        return "req-" + UUID.randomUUID();
    }
}
