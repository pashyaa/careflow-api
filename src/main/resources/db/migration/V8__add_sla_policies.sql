-- CF-106: configurable SLA policies, site working calendars, per-work-order SLA snapshot.

-- 1. Site working calendar (wall-clock times in the site's own time zone) -------------
ALTER TABLE service_sites
    ADD COLUMN time_zone      VARCHAR(60) NOT NULL DEFAULT 'Asia/Kolkata',
    ADD COLUMN business_start TIME        NOT NULL DEFAULT '09:00',
    ADD COLUMN business_end   TIME        NOT NULL DEFAULT '18:00',
    ADD COLUMN business_days  VARCHAR(40) NOT NULL DEFAULT 'MON,TUE,WED,THU,FRI',
    ADD CONSTRAINT chk_service_sites_business_hours CHECK (business_start < business_end);

-- 2. Policies. NULL site/category/priority means "applies to any". ---------------------
CREATE TABLE sla_policies (
                              id                  UUID PRIMARY KEY,
                              organization_id     UUID         NOT NULL REFERENCES organizations(id),
                              name                VARCHAR(120) NOT NULL,
                              site_id             UUID         REFERENCES service_sites(id),
                              category            VARCHAR(80),
                              priority            VARCHAR(20),
                              response_minutes    INTEGER      NOT NULL,
                              resolution_minutes  INTEGER      NOT NULL,
                              business_hours_only BOOLEAN      NOT NULL DEFAULT FALSE,
                              active              BOOLEAN      NOT NULL DEFAULT TRUE,
                              created_at          TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              updated_at          TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              CONSTRAINT chk_sla_policy_priority CHECK (priority IS NULL OR priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
                              CONSTRAINT chk_sla_policy_minutes CHECK (response_minutes > 0 AND resolution_minutes >= response_minutes)
);

CREATE INDEX idx_sla_policies_organization ON sla_policies(organization_id);

-- Only one ACTIVE policy per exact scope, so precedence can never tie.
CREATE UNIQUE INDEX uq_sla_policies_active_scope ON sla_policies (
                                                                  organization_id,
                                                                  COALESCE(site_id, '00000000-0000-0000-0000-000000000000'::uuid),
                                                                  COALESCE(LOWER(category), ''),
                                                                  COALESCE(priority, '')
    ) WHERE active;

-- 3. Holidays: site_id NULL = the whole organization. -------------------------------------
CREATE TABLE sla_holidays (
                              id              UUID PRIMARY KEY,
                              organization_id UUID         NOT NULL REFERENCES organizations(id),
                              site_id         UUID         REFERENCES service_sites(id),
                              holiday_date    DATE         NOT NULL,
                              name            VARCHAR(120) NOT NULL
);
CREATE INDEX idx_sla_holidays_org_date ON sla_holidays(organization_id, holiday_date);

-- 4. Snapshot columns on work orders. target_resolution_at stays and now holds the
--    calculated resolution deadline, so sorting and existing screens keep working.
ALTER TABLE work_orders
    ADD COLUMN sla_policy_id           UUID REFERENCES sla_policies(id),
    ADD COLUMN sla_policy_name         VARCHAR(120),
    ADD COLUMN sla_policy_scope        VARCHAR(40),
    ADD COLUMN sla_response_minutes    INTEGER,
    ADD COLUMN sla_resolution_minutes  INTEGER,
    ADD COLUMN sla_business_hours_only BOOLEAN,
    ADD COLUMN sla_time_zone           VARCHAR(60),
    ADD COLUMN response_due_at         TIMESTAMPTZ,
    ADD COLUMN response_at_risk_at     TIMESTAMPTZ,
    ADD COLUMN resolution_at_risk_at   TIMESTAMPTZ,
    ADD COLUMN first_response_at       TIMESTAMPTZ;

-- Existing rows have no policy snapshot (they keep their hand-picked target). Backfill
-- first_response_at from the audit trail so they don't look "unresponded".
UPDATE work_orders w SET first_response_at = (
    SELECT MIN(h.changed_at) FROM work_order_status_history h
    WHERE h.work_order_id = w.id AND h.to_status NOT IN ('NEW', 'CANCELLED')
);

CREATE INDEX idx_work_orders_org_sla ON work_orders(organization_id, status, target_resolution_at);

-- 5. Seed policies. Tenant 1 (Meridian) ------------------------------------------------------
INSERT INTO sla_policies (id, organization_id, name, site_id, category, priority, response_minutes, resolution_minutes, business_hours_only) VALUES
                                                                                                                                                 ('80000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', 'Critical - default',            NULL, NULL,   'CRITICAL',  30,  240, FALSE),
                                                                                                                                                 ('80000000-0000-0000-0000-000000000002', '70000000-0000-0000-0000-000000000001', 'High - default',                NULL, NULL,   'HIGH',     120,  480, TRUE),
                                                                                                                                                 ('80000000-0000-0000-0000-000000000003', '70000000-0000-0000-0000-000000000001', 'Medium - default',              NULL, NULL,   'MEDIUM',   240, 1440, TRUE),
                                                                                                                                                 ('80000000-0000-0000-0000-000000000004', '70000000-0000-0000-0000-000000000001', 'Low - default',                 NULL, NULL,   'LOW',      480, 2880, TRUE),
                                                                                                                                                 ('80000000-0000-0000-0000-000000000005', '70000000-0000-0000-0000-000000000001', 'Tenant fallback',               NULL, NULL,   NULL,       480, 4320, TRUE),
                                                                                                                                                 ('80000000-0000-0000-0000-000000000006', '70000000-0000-0000-0000-000000000001', 'Hyderabad campus - critical',   '10000000-0000-0000-0000-000000000002', NULL, 'CRITICAL', 15, 120, FALSE),
                                                                                                                                                 ('80000000-0000-0000-0000-000000000007', '70000000-0000-0000-0000-000000000001', 'HVAC - critical',               NULL, 'HVAC', 'CRITICAL',  20,  180, FALSE);

-- Tenant 2 (Solstice)
INSERT INTO sla_policies (id, organization_id, name, site_id, category, priority, response_minutes, resolution_minutes, business_hours_only) VALUES
                                                                                                                                                 ('80000000-0000-0000-0000-000000000101', '70000000-0000-0000-0000-000000000002', 'Critical - default', NULL, NULL, 'CRITICAL',  30,  240, FALSE),
                                                                                                                                                 ('80000000-0000-0000-0000-000000000102', '70000000-0000-0000-0000-000000000002', 'High - default',     NULL, NULL, 'HIGH',     120,  480, TRUE),
                                                                                                                                                 ('80000000-0000-0000-0000-000000000103', '70000000-0000-0000-0000-000000000002', 'Medium - default',   NULL, NULL, 'MEDIUM',   240, 1440, TRUE),
                                                                                                                                                 ('80000000-0000-0000-0000-000000000104', '70000000-0000-0000-0000-000000000002', 'Low - default',      NULL, NULL, 'LOW',      480, 2880, TRUE),
                                                                                                                                                 ('80000000-0000-0000-0000-000000000105', '70000000-0000-0000-0000-000000000002', 'Tenant fallback',    NULL, NULL, NULL,       480, 4320, TRUE);

-- 6. Seed holidays (organization-wide, fixed-date national holidays)
INSERT INTO sla_holidays (id, organization_id, site_id, holiday_date, name) VALUES
                                                                                ('81000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', NULL, DATE '2026-12-25', 'Christmas Day'),
                                                                                ('81000000-0000-0000-0000-000000000002', '70000000-0000-0000-0000-000000000001', NULL, DATE '2027-01-26', 'Republic Day'),
                                                                                ('81000000-0000-0000-0000-000000000003', '70000000-0000-0000-0000-000000000001', NULL, DATE '2027-08-15', 'Independence Day'),
                                                                                ('81000000-0000-0000-0000-000000000004', '70000000-0000-0000-0000-000000000002', NULL, DATE '2026-12-25', 'Christmas Day'),
                                                                                ('81000000-0000-0000-0000-000000000005', '70000000-0000-0000-0000-000000000002', NULL, DATE '2027-01-26', 'Republic Day');