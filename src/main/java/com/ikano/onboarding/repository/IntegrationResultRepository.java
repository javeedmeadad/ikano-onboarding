package com.ikano.onboarding.repository;

import com.ikano.onboarding.domain.IntegrationType;
import com.ikano.onboarding.entity.IntegrationResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IntegrationResultRepository extends JpaRepository<IntegrationResultEntity, UUID> {

    List<IntegrationResultEntity> findByApplicationIdOrderByCheckedAtAsc(UUID applicationId);

    Optional<IntegrationResultEntity> findTopByApplicationIdAndIntegrationTypeOrderByCheckedAtDesc(
            UUID applicationId, IntegrationType integrationType);
}
