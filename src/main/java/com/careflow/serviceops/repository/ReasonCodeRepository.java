package com.careflow.serviceops.repository;

import com.careflow.serviceops.domain.ReasonCategory;
import com.careflow.serviceops.domain.ReasonCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReasonCodeRepository extends JpaRepository<ReasonCode, UUID> {

    List<ReasonCode> findByCategoryAndActiveTrueOrderBySortOrderAsc(ReasonCategory category);

    Optional<ReasonCode> findByCategoryAndCodeAndActiveTrue(ReasonCategory category, String code);
}