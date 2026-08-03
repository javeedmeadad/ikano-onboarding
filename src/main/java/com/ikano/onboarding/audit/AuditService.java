package com.ikano.onboarding.audit;

import com.ikano.onboarding.entity.AuditEntryEntity;
import com.ikano.onboarding.repository.AuditEntryRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Records what was checked, when, and with what result — never the underlying sensitive answers
 * (no names, ID numbers, income figures). Every entry carries a request/event id so a support
 * person can trace one application's history end to end.
 */
@Service
public class AuditService {

    private final AuditEntryRepository repository;

    public AuditService(AuditEntryRepository repository) {
        this.repository = repository;
    }

    public void log(UUID applicationId, String requestId, String eventType, String description) {
        AuditEntryEntity entry = new AuditEntryEntity();
        entry.setApplicationId(applicationId);
        entry.setRequestId(requestId);
        entry.setEventType(eventType);
        entry.setDescription(description);
        repository.save(entry);
    }

    public List<AuditEntryEntity> history(UUID applicationId) {
        return repository.findByApplicationIdOrderByOccurredAtAsc(applicationId);
    }
}
