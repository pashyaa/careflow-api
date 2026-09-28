package com.careflow.serviceops.repository;

import com.careflow.serviceops.domain.WorkOrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WorkOrderStatusHistoryRepository extends JpaRepository<WorkOrderStatusHistory, UUID> {
    // CF-102: scoped by organizationId even though the workOrder is already loaded
    // tenant-scoped by the caller — defense in depth, so this query alone (e.g. if
    // ever called from a new code path) can never cross a tenant boundary.
    List<WorkOrderStatusHistory> findByWorkOrderIdAndOrganizationIdOrderByChangedAtAsc(
            UUID workOrderId, UUID organizationId);
}