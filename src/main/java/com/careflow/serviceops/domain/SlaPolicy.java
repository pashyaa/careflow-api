package com.careflow.serviceops.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A tenant-owned SLA rule. null site / category / priority means "applies to any".
 *
 * Precedence (highest score wins): site = 4, category = 2, priority = 1, tenant
 * fallback (all null) = 0. So site+priority (5) > site (4) > category+priority (3)
 * > category (2) > priority (1) > tenant fallback (0).
 */
@Entity
@Table(name = "sla_policies")
public class SlaPolicy {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "site_id")
    private UUID siteId;

    @Column(length = 80)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Priority priority;

    @Column(name = "response_minutes", nullable = false)
    private int responseMinutes;

    @Column(name = "resolution_minutes", nullable = false)
    private int resolutionMinutes;

    @Column(name = "business_hours_only", nullable = false)
    private boolean businessHoursOnly;

    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected SlaPolicy() {
    }

    public SlaPolicy(UUID organizationId, String name, UUID siteId, String category, Priority priority,
                     int responseMinutes, int resolutionMinutes, boolean businessHoursOnly, boolean active) {
        this.organizationId = organizationId;
        update(name, siteId, category, priority, responseMinutes, resolutionMinutes, businessHoursOnly, active);
    }

    public void update(String name, UUID siteId, String category, Priority priority,
                       int responseMinutes, int resolutionMinutes, boolean businessHoursOnly, boolean active) {
        this.name = name;
        this.siteId = siteId;
        this.category = normalizeCategory(category);
        this.priority = priority;
        this.responseMinutes = responseMinutes;
        this.resolutionMinutes = resolutionMinutes;
        this.businessHoursOnly = businessHoursOnly;
        this.active = active;
    }

    public static String normalizeCategory(String category) {
        return category == null || category.isBlank() ? null : category.trim();
    }

    /** True when this policy applies to a work order with the given site, asset category and priority. */
    public boolean matches(UUID workOrderSiteId, String assetCategory, Priority workOrderPriority) {
        if (!active) return false;
        if (siteId != null && !siteId.equals(workOrderSiteId)) return false;
        if (category != null && (assetCategory == null || !category.equalsIgnoreCase(assetCategory))) return false;
        return priority == null || priority == workOrderPriority;
    }

    public int specificity() {
        return (siteId != null ? 4 : 0) + (category != null ? 2 : 0) + (priority != null ? 1 : 0);
    }

    /** Human/machine readable description of which dimensions this policy is pinned to. */
    public String scopeLabel() {
        List<String> parts = new ArrayList<>();
        if (siteId != null) parts.add("SITE");
        if (category != null) parts.add("CATEGORY");
        if (priority != null) parts.add("PRIORITY");
        return parts.isEmpty() ? "TENANT_DEFAULT" : String.join("+", parts);
    }

    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organizationId; }
    public String getName() { return name; }
    public UUID getSiteId() { return siteId; }
    public String getCategory() { return category; }
    public Priority getPriority() { return priority; }
    public int getResponseMinutes() { return responseMinutes; }
    public int getResolutionMinutes() { return resolutionMinutes; }
    public boolean isBusinessHoursOnly() { return businessHoursOnly; }
    public boolean isActive() { return active; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}