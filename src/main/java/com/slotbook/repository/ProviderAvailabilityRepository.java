package com.slotbook.repository;

import com.slotbook.domain.ProviderAvailabilityEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProviderAvailabilityRepository extends JpaRepository<ProviderAvailabilityEntity, Long> {

    Optional<ProviderAvailabilityEntity> findByProviderId(Long providerId);
}