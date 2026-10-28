package com.careflow.serviceops.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "work_order_status_history")
public class WorkOrderStatusHistory {

    @Id
    @UuidGenerator
    private UUID id;

    // CF-102: denormalized from the parent work order so audit-trail reads can be
    // tenant-scoped directly, without relying solely on the work_order_id join.
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_order_id", nullable = false)
    private WorkOrder workOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 30)
    private WorkOrderStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 30)
    private WorkOrderStatus toStatus;

    // BUG-305: set only on RESOLVED / CANCELLED transitions. The label is copied at write
    // time so the audit trail stays readable if a reason code is later renamed.
    @Column(name = "reason_code", length = 40)
    private String reasonCode;

    @Column(name = "reason_label", length = 120)
    private String reasonLabel;

    @Column(length = 500)
    private String note;

    // The acting user's id, resolved server-side from the authenticated principal —
    // never accepted from the client. Nullable only to accommodate historic/system
    // rows written before this column existed (see V5 migration); every row written
    // by the application from now on populates it.
    @Column(name = "changed_by_user_id")
    private UUID changedByUserId;

    // Denormalized at write time so the audit trail still reads sensibly even if the
    // acting user is later renamed or deactivated.
    @Column(name = "changed_by_display_name", nullable = false, length = 120)
    private String changedByDisplayName;

    @CreationTimestamp
    @Column(name = "changed_at", nullable = false, updatable = false)
    private OffsetDateTime changedAt;

    protected WorkOrderStatusHistory() {
    }

    public WorkOrderStatusHistory(WorkOrder workOrder, WorkOrderStatus fromStatus, WorkOrderStatus toStatus,
                                  String note, UUID changedByUserId, String changedByDisplayName) {
        this(workOrder, fromStatus, toStatus, note, null, null, changedByUserId, changedByDisplayName);
    }

    public WorkOrderStatusHistory(WorkOrder workOrder, WorkOrderStatus fromStatus, WorkOrderStatus toStatus,
                                  String note, String reasonCode, String reasonLabel,
                                  UUID changedByUserId, String changedByDisplayName) {
        this.organizationId = workOrder.getOrganizationId();
        this.workOrder = workOrder;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.note = note;
        this.reasonCode = reasonCode;
        this.reasonLabel = reasonLabel;
        this.changedByUserId = changedByUserId;
        this.changedByDisplayName = changedByDisplayName;
    }

    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organizationId; }
    public WorkOrderStatus getFromStatus() { return fromStatus; }
    public WorkOrderStatus getToStatus() { return toStatus; }
    public String getNote() { return note; }
    public String getReasonCode() { return reasonCode; }
    public String getReasonLabel() { return reasonLabel; }
    public UUID getChangedByUserId() { return changedByUserId; }
    public String getChangedByDisplayName() { return changedByDisplayName; }
    public OffsetDateTime getChangedAt() { return changedAt; }
}