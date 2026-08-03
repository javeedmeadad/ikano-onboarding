package com.ikano.onboarding.repository;

import com.ikano.onboarding.entity.StepRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StepRecordRepository extends JpaRepository<StepRecordEntity, UUID> {

    List<StepRecordEntity> findByApplicationIdOrderByCompletedAtAsc(UUID applicationId);

    Optional<StepRecordEntity> findByApplicationIdAndStepKey(UUID applicationId, String stepKey);
}
