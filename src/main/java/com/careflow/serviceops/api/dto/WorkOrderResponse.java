package com.careflow.serviceops.api.dto;

import com.careflow.serviceops.domain.Priority;
import com.careflow.serviceops.domain.WorkOrderStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record WorkOrderResponse(
        UUID id,
        String referenceNumber,
        String title,
        String description,
        Priority priority,
        WorkOrderStatus status,
        SiteView site,
        AssetView asset,
        TechnicianView assignedTechnician,
        OffsetDateTime targetResolutionAt,
        OffsetDateTime resolvedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        long version
) {
    public record SiteView(UUID id, String code, String name, String customerName, String city) { }
    public record AssetView(UUID id, String tag, String name, String category, String status) { }
    public record TechnicianView(UUID id, String employeeCode, String name, String email, String primarySkill) { }
}

