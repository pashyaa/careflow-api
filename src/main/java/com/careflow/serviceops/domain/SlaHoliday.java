package com.careflow.serviceops.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.util.UUID;

/** A non-working day. siteId == null means the holiday applies to the whole organization. */
@Entity
@Table(name = "sla_holidays")
public class SlaHoliday {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "site_id")
    private UUID siteId;

    @Column(name = "holiday_date", nullable = false)
    private LocalDate holidayDate;

    @Column(nullable = false, length = 120)
    private String name;

    protected SlaHoliday() {
    }

    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organizationId; }
    public UUID getSiteId() { return siteId; }
    public LocalDate getHolidayDate() { return holidayDate; }
    public String getName() { return name; }
}