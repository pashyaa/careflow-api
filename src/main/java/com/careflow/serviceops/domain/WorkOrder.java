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

    @Column(name = "reference_number", nullable = false, unique = true, length = 40)
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

    public WorkOrder(String referenceNumber, String title, String description, Priority priority,
                     ServiceSite site, Asset asset, OffsetDateTime targetResolutionAt) {
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
    }

    public UUID getId() { return id; }
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
}

