package com.careflow.serviceops.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "reason_codes")
public class ReasonCode {

    @Id
    @UuidGenerator
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReasonCategory category;

    @Column(nullable = false, length = 40)
    private String code;

    @Column(nullable = false, length = 120)
    private String label;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(nullable = false)
    private boolean active = true;

    protected ReasonCode() {
    }

    public ReasonCode(ReasonCategory category, String code, String label, int sortOrder) {
        this.category = category;
        this.code = code;
        this.label = label;
        this.sortOrder = sortOrder;
    }

    public UUID getId() { return id; }
    public ReasonCategory getCategory() { return category; }
    public String getCode() { return code; }
    public String getLabel() { return label; }
    public int getSortOrder() { return sortOrder; }
    public boolean isActive() { return active; }
}