package com.careflow.serviceops.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "service_sites")
public class ServiceSite {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "site_code", nullable = false, unique = true, length = 30)
    private String siteCode;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "customer_name", nullable = false, length = 160)
    private String customerName;

    @Column(name = "address_line_1", nullable = false, length = 180)
    private String addressLine1;

    @Column(nullable = false, length = 80)
    private String city;

    @Column(nullable = false, length = 80)
    private String state;

    @Column(name = "postal_code", nullable = false, length = 20)
    private String postalCode;

    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected ServiceSite() {
    }

    public UUID getId() { return id; }
    public String getSiteCode() { return siteCode; }
    public String getName() { return name; }
    public String getCustomerName() { return customerName; }
    public String getAddressLine1() { return addressLine1; }
    public String getCity() { return city; }
    public String getState() { return state; }
    public String getPostalCode() { return postalCode; }
    public boolean isActive() { return active; }
}

