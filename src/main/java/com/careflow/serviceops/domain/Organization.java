package com.careflow.serviceops.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A service-provider organization onboarded onto CareFlow — the tenant boundary for
 * every operational record (sites, assets, technicians, work orders, users, ...).
 * CF-102: every row that belongs to a tenant carries this id, and every repository
 * query that reads or writes those rows is scoped by it.
 */
@Entity
@Table(name = "organizations")
public class Organization {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(nullable = false, unique = true, length = 60)
    private String slug;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected Organization() {
    }

    public UUID getId() { return id; }
    public String getSlug() { return slug; }
    public String getName() { return name; }
    public boolean isActive() { return active; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}