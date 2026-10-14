package com.careflow.serviceops.repository;

import com.careflow.serviceops.domain.SlaPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SlaPolicyRepository extends JpaRepository<SlaPolicy, UUID> {

    List<SlaPolicy> findByOrganizationIdAndActiveTrue(UUID organizationId);

    List<SlaPolicy> findByOrganizationIdOrderByNameAsc(UUID organizationId);

    Optional<SlaPolicy> findByIdAndOrganizationId(UUID id, UUID organizationId);
}