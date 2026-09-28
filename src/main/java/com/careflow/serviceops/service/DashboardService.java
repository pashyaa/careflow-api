package com.careflow.serviceops.service;

import com.careflow.serviceops.api.dto.DashboardSummaryResponse;
import com.careflow.serviceops.domain.Priority;
import com.careflow.serviceops.domain.WorkOrderStatus;
import com.careflow.serviceops.repository.WorkOrderRepository;
import com.careflow.serviceops.security.CurrentActorResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private static final Set<WorkOrderStatus> OPEN_STATUSES = EnumSet.of(
            WorkOrderStatus.NEW, WorkOrderStatus.ASSIGNED, WorkOrderStatus.IN_PROGRESS, WorkOrderStatus.ON_HOLD
    );

    private final WorkOrderRepository repository;
    private final CurrentActorResolver currentActorResolver;

    public DashboardService(WorkOrderRepository repository, CurrentActorResolver currentActorResolver) {
        this.repository = repository;
        this.currentActorResolver = currentActorResolver;
    }

    public DashboardSummaryResponse summary() {
        // CF-102: every aggregate below is scoped to the caller's own tenant — without
        // this, the dashboard would silently blend counts across every tenant in the
        // database, which is a worse leak than a single record because it's invisible.
        UUID organizationId = currentActorResolver.resolve().organizationId();

        Map<String, Long> breakdown = new LinkedHashMap<>();
        Arrays.stream(WorkOrderStatus.values()).forEach(status ->
                breakdown.put(status.name(), repository.countByOrganizationIdAndStatus(organizationId, status)));

        return new DashboardSummaryResponse(
                repository.countByOrganizationIdAndStatusIn(organizationId, OPEN_STATUSES),
                repository.countByOrganizationIdAndTargetResolutionAtBeforeAndStatusIn(
                        organizationId, OffsetDateTime.now(), OPEN_STATUSES),
                repository.countByOrganizationIdAndStatusInAndAssignedTechnicianIsNull(organizationId, OPEN_STATUSES),
                repository.countByOrganizationIdAndPriorityAndStatusIn(organizationId, Priority.CRITICAL, OPEN_STATUSES),
                breakdown,
                OffsetDateTime.now()
        );
    }
}