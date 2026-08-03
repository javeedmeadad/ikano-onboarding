package com.ikano.onboarding.repository;

import com.ikano.onboarding.entity.OnboardingApplicationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OnboardingApplicationRepository extends JpaRepository<OnboardingApplicationEntity, UUID> {

    Optional<OnboardingApplicationEntity> findByResumeToken(String resumeToken);
}
