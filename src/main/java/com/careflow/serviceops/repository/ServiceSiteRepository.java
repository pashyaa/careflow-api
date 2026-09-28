package com.careflow.serviceops.repository;

import com.careflow.serviceops.domain.ServiceSite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceSiteRepository extends JpaRepository<ServiceSite, UUID> {

    // CF-102: every finder is scoped by organizationId. There is deliberately no
    // findById(UUID) call site left anywhere in the service layer for this entity —
    // see ServiceSiteRepository usages in WorkOrderService.
    List<ServiceSite> findByOrganizationIdAndActiveTrueOrderByNameAsc(UUID organizationId);

    Optional<ServiceSite> findByIdAndOrganizationId(UUID id, UUID organizationId);
}