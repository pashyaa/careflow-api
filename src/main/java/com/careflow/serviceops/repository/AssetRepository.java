package com.careflow.serviceops.repository;

import com.careflow.serviceops.domain.Asset;
import com.careflow.serviceops.domain.AssetStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssetRepository extends JpaRepository<Asset, UUID> {

    // CF-102: scoped by organizationId in addition to siteId, so a caller can never
    // list assets belonging to a site in a different tenant even if it somehow got
    // hold of that site's id.
    List<Asset> findBySiteIdAndOrganizationIdAndStatusNotInOrderByNameAsc(
            UUID siteId, UUID organizationId, Collection<AssetStatus> statuses);

    Optional<Asset> findByIdAndOrganizationId(UUID id, UUID organizationId);
}