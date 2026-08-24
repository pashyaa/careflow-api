package com.careflow.serviceops.api.dto;

import com.careflow.serviceops.domain.WorkOrderStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record HistoryResponse(
        UUID id,
        WorkOrderStatus fromStatus,
        WorkOrderStatus toStatus,
        String note,
        String changedBy,
        OffsetDateTime changedAt
) {
}

