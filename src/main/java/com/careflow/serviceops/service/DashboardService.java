package com.careflow.serviceops.service;

import com.careflow.serviceops.api.dto.DashboardSummaryResponse;
import com.careflow.serviceops.domain.Priority;
import com.careflow.serviceops.domain.WorkOrderStatus;
import com.careflow.serviceops.repository.WorkOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private static final Set<WorkOrderStatus> OPEN_STATUSES = EnumSet.of(
            WorkOrderStatus.NEW, WorkOrderStatus.ASSIGNED, WorkOrderStatus.IN_PROGRESS, WorkOrderStatus.ON_HOLD
    );

    private final WorkOrderRepository repository;

    public DashboardService(WorkOrderRepository repository) {
        this.repository = repository;
    }

    public DashboardSummaryResponse summary() {
        Map<String, Long> breakdown = new LinkedHashMap<>();
        Arrays.stream(WorkOrderStatus.values()).forEach(status ->
                breakdown.put(status.name(), repository.countByStatus(status)));

        return new DashboardSummaryResponse(
                repository.countByStatusIn(OPEN_STATUSES),
                repository.countByTargetResolutionAtBeforeAndStatusIn(OffsetDateTime.now(), OPEN_STATUSES),
                repository.countByStatusInAndAssignedTechnicianIsNull(OPEN_STATUSES),
                repository.countByPriorityAndStatusIn(Priority.CRITICAL, OPEN_STATUSES),
                breakdown,
                OffsetDateTime.now()
        );
    }
}

