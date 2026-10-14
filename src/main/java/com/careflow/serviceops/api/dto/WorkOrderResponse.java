package com.careflow.serviceops.api.dto;

import com.careflow.serviceops.domain.Priority;
import com.careflow.serviceops.domain.SlaState;
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
        long version,
        SlaView sla
) {
    /** Backwards-compatible constructor for callers that don't care about SLA data. */
    public WorkOrderResponse(UUID id, String referenceNumber, String title, String description, Priority priority,
                             WorkOrderStatus status, SiteView site, AssetView asset,
                             TechnicianView assignedTechnician, OffsetDateTime targetResolutionAt,
                             OffsetDateTime resolvedAt, OffsetDateTime createdAt, OffsetDateTime updatedAt,
                             long version) {
        this(id, referenceNumber, title, description, priority, status, site, asset, assignedTechnician,
                targetResolutionAt, resolvedAt, createdAt, updatedAt, version, null);
    }

    public record SiteView(UUID id, String code, String name, String customerName, String city) { }
    public record AssetView(UUID id, String tag, String name, String category, String status) { }
    public record TechnicianView(UUID id, String employeeCode, String name, String email, String primarySkill) { }

    /** The SLA snapshot taken at creation plus its live state. Policy fields are null on legacy work orders. */
    public record SlaView(
            UUID policyId,
            String policyName,
            String policyScope,
            Integer responseMinutes,
            Integer resolutionMinutes,
            Boolean businessHoursOnly,
            String timeZone,
            OffsetDateTime responseDueAt,
            OffsetDateTime firstResponseAt,
            SlaState responseState,
            OffsetDateTime resolutionDueAt,
            OffsetDateTime resolvedAt,
            SlaState resolutionState,
            SlaState overallState
    ) { }
}