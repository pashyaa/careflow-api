package com.careflow.serviceops.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "work_orders", indexes = {
        @Index(name = "idx_work_orders_status", columnList = "status"),
        @Index(name = "idx_work_orders_target_resolution", columnList = "target_resolution_at")
})
public class WorkOrder {

    @Id
    @UuidGenerator
    private UUID id;

    // CF-102: denormalized from the owning site at creation time so every query
    // (list, count, dashboard) can filter on this column directly without a join.
    // reference_number is only unique WITHIN a tenant now (see V6 migration).
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "reference_number", nullable = false, length = 40)
    private String referenceNumber;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(nullable = false, length = 4000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private WorkOrderStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "site_id", nullable = false)
    private ServiceSite site;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id")
    private Asset asset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_technician_id")
    private Technician assignedTechnician;

    @Column(name = "target_resolution_at", nullable = false)
    private OffsetDateTime targetResolutionAt;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    // CF-106: snapshot of the SLA policy chosen at creation. Later edits to the policy
    // never change these values. target_resolution_at above doubles as the resolution deadline.
    @Column(name = "sla_policy_id")
    private UUID slaPolicyId;

    @Column(name = "sla_policy_name", length = 120)
    private String slaPolicyName;

    @Column(name = "sla_policy_scope", length = 40)
    private String slaPolicyScope;

    @Column(name = "sla_response_minutes")
    private Integer slaResponseMinutes;

    @Column(name = "sla_resolution_minutes")
    private Integer slaResolutionMinutes;

    @Column(name = "sla_business_hours_only")
    private Boolean slaBusinessHoursOnly;

    @Column(name = "sla_time_zone", length = 60)
    private String slaTimeZone;

    @Column(name = "response_due_at")
    private OffsetDateTime responseDueAt;

    @Column(name = "response_at_risk_at")
    private OffsetDateTime responseAtRiskAt;

    @Column(name = "resolution_at_risk_at")
    private OffsetDateTime resolutionAtRiskAt;

    @Column(name = "first_response_at")
    private OffsetDateTime firstResponseAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected WorkOrder() {
    }

    public WorkOrder(UUID organizationId, String referenceNumber, String title, String description, Priority priority,
                     ServiceSite site, Asset asset, OffsetDateTime targetResolutionAt) {
        this.organizationId = organizationId;
        this.referenceNumber = referenceNumber;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.site = site;
        this.asset = asset;
        this.targetResolutionAt = targetResolutionAt;
        this.status = WorkOrderStatus.NEW;
    }

    public void assignTo(Technician technician) { this.assignedTechnician = technician; }
    public void changeStatus(WorkOrderStatus status) {
        this.status = status;
        this.resolvedAt = status == WorkOrderStatus.RESOLVED ? OffsetDateTime.now() : null;
        // CF-106: the response SLA stops the first time work moves past NEW.
        if (firstResponseAt == null && status != WorkOrderStatus.NEW && status != WorkOrderStatus.CANCELLED) {
            firstResponseAt = OffsetDateTime.now();
        }
    }

    /** CF-106: copies the chosen policy and computed deadlines onto this work order. */
    public void applySla(SlaPlan plan) {
        this.slaPolicyId = plan.policyId();
        this.slaPolicyName = plan.policyName();
        this.slaPolicyScope = plan.policyScope();
        this.slaResponseMinutes = plan.responseMinutes();
        this.slaResolutionMinutes = plan.resolutionMinutes();
        this.slaBusinessHoursOnly = plan.businessHoursOnly();
        this.slaTimeZone = plan.timeZone();
        this.responseDueAt = plan.responseDueAt();
        this.responseAtRiskAt = plan.responseAtRiskAt();
        this.resolutionAtRiskAt = plan.resolutionAtRiskAt();
        this.targetResolutionAt = plan.resolutionDueAt();
    }

    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organizationId; }
    public String getReferenceNumber() { return referenceNumber; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Priority getPriority() { return priority; }
    public WorkOrderStatus getStatus() { return status; }
    public ServiceSite getSite() { return site; }
    public Asset getAsset() { return asset; }
    public Technician getAssignedTechnician() { return assignedTechnician; }
    public OffsetDateTime getTargetResolutionAt() { return targetResolutionAt; }
    public OffsetDateTime getResolvedAt() { return resolvedAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
    public UUID getSlaPolicyId() { return slaPolicyId; }
    public String getSlaPolicyName() { return slaPolicyName; }
    public String getSlaPolicyScope() { return slaPolicyScope; }
    public Integer getSlaResponseMinutes() { return slaResponseMinutes; }
    public Integer getSlaResolutionMinutes() { return slaResolutionMinutes; }
    public Boolean getSlaBusinessHoursOnly() { return slaBusinessHoursOnly; }
    public String getSlaTimeZone() { return slaTimeZone; }
    public OffsetDateTime getResponseDueAt() { return responseDueAt; }
    public OffsetDateTime getResponseAtRiskAt() { return responseAtRiskAt; }
    public OffsetDateTime getResolutionAtRiskAt() { return resolutionAtRiskAt; }
    public OffsetDateTime getFirstResponseAt() { return firstResponseAt; }
}