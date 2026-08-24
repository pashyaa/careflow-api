package com.careflow.serviceops.repository;

import com.careflow.serviceops.domain.Asset;
import com.careflow.serviceops.domain.AssetStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AssetRepository extends JpaRepository<Asset, UUID> {
    List<Asset> findBySiteIdAndStatusNotInOrderByNameAsc(UUID siteId, Collection<AssetStatus> statuses);
}

