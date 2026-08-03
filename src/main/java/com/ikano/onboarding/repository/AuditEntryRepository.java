package com.ikano.onboarding.repository;

import com.ikano.onboarding.entity.AuditEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditEntryRepository extends JpaRepository<AuditEntryEntity, UUID> {

    List<AuditEntryEntity> findByApplicationIdOrderByOccurredAtAsc(UUID applicationId);
}
