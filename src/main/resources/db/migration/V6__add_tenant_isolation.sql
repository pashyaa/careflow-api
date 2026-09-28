-- CF-102: model service-provider organizations (tenants) and scope every
-- operational record to one. This migration:
--   1. creates the organizations table
--   2. seeds the tenant that owns all pre-existing data ("Meridian Facilities
--      Services") so nothing already in the database is orphaned
--   3. adds organization_id to every operational table, backfills it, then makes
--      it NOT NULL and FK-constrained
--   4. replaces globally-unique natural keys (site_code, asset_tag,
--      employee_code, technician email, work order reference_number) with
--      composite keys unique WITHIN a tenant, since two tenants may legitimately
--      reuse the same codes
--   5. adds indexes so tenant-scoped queries stay fast

CREATE TABLE organizations (
                               id UUID PRIMARY KEY,
                               slug VARCHAR(60) NOT NULL UNIQUE,
                               name VARCHAR(160) NOT NULL,
                               active BOOLEAN NOT NULL DEFAULT TRUE,
                               created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO organizations (id, slug, name, created_at) VALUES
    ('70000000-0000-0000-0000-000000000001', 'meridian-facilities', 'Meridian Facilities Services', CURRENT_TIMESTAMP);

-- ---------------------------------------------------------------------------
-- users
-- ---------------------------------------------------------------------------
ALTER TABLE users ADD COLUMN organization_id UUID;
UPDATE users SET organization_id = '70000000-0000-0000-0000-000000000001';
ALTER TABLE users ALTER COLUMN organization_id SET NOT NULL;
ALTER TABLE users ADD CONSTRAINT fk_users_organization
    FOREIGN KEY (organization_id) REFERENCES organizations(id);
CREATE INDEX idx_users_organization ON users(organization_id);

-- ---------------------------------------------------------------------------
-- service_sites
-- ---------------------------------------------------------------------------
ALTER TABLE service_sites ADD COLUMN organization_id UUID;
UPDATE service_sites SET organization_id = '70000000-0000-0000-0000-000000000001';
ALTER TABLE service_sites ALTER COLUMN organization_id SET NOT NULL;
ALTER TABLE service_sites ADD CONSTRAINT fk_service_sites_organization
    FOREIGN KEY (organization_id) REFERENCES organizations(id);

ALTER TABLE service_sites DROP CONSTRAINT service_sites_site_code_key;
ALTER TABLE service_sites ADD CONSTRAINT uq_service_sites_org_site_code UNIQUE (organization_id, site_code);
CREATE INDEX idx_service_sites_organization ON service_sites(organization_id);

-- ---------------------------------------------------------------------------
-- assets
-- ---------------------------------------------------------------------------
ALTER TABLE assets ADD COLUMN organization_id UUID;
UPDATE assets a SET organization_id = s.organization_id
    FROM service_sites s WHERE a.site_id = s.id;
ALTER TABLE assets ALTER COLUMN organization_id SET NOT NULL;
ALTER TABLE assets ADD CONSTRAINT fk_assets_organization
    FOREIGN KEY (organization_id) REFERENCES organizations(id);

ALTER TABLE assets DROP CONSTRAINT assets_asset_tag_key;
ALTER TABLE assets ADD CONSTRAINT uq_assets_org_asset_tag UNIQUE (organization_id, asset_tag);
CREATE INDEX idx_assets_organization ON assets(organization_id);
CREATE INDEX idx_assets_org_site ON assets(organization_id, site_id);

-- ---------------------------------------------------------------------------
-- technicians
-- ---------------------------------------------------------------------------
ALTER TABLE technicians ADD COLUMN organization_id UUID;
UPDATE technicians SET organization_id = '70000000-0000-0000-0000-000000000001';
ALTER TABLE technicians ALTER COLUMN organization_id SET NOT NULL;
ALTER TABLE technicians ADD CONSTRAINT fk_technicians_organization
    FOREIGN KEY (organization_id) REFERENCES organizations(id);

ALTER TABLE technicians DROP CONSTRAINT technicians_employee_code_key;
ALTER TABLE technicians DROP CONSTRAINT technicians_email_key;
ALTER TABLE technicians ADD CONSTRAINT uq_technicians_org_employee_code UNIQUE (organization_id, employee_code);
ALTER TABLE technicians ADD CONSTRAINT uq_technicians_org_email UNIQUE (organization_id, email);
CREATE INDEX idx_technicians_organization ON technicians(organization_id);

-- ---------------------------------------------------------------------------
-- work_orders
-- ---------------------------------------------------------------------------
ALTER TABLE work_orders ADD COLUMN organization_id UUID;
UPDATE work_orders w SET organization_id = s.organization_id
    FROM service_sites s WHERE w.site_id = s.id;
ALTER TABLE work_orders ALTER COLUMN organization_id SET NOT NULL;
ALTER TABLE work_orders ADD CONSTRAINT fk_work_orders_organization
    FOREIGN KEY (organization_id) REFERENCES organizations(id);

ALTER TABLE work_orders DROP CONSTRAINT work_orders_reference_number_key;
ALTER TABLE work_orders ADD CONSTRAINT uq_work_orders_org_reference_number UNIQUE (organization_id, reference_number);
CREATE INDEX idx_work_orders_organization ON work_orders(organization_id);
CREATE INDEX idx_work_orders_org_status ON work_orders(organization_id, status);

-- ---------------------------------------------------------------------------
-- work_order_status_history
-- ---------------------------------------------------------------------------
ALTER TABLE work_order_status_history ADD COLUMN organization_id UUID;
UPDATE work_order_status_history h SET organization_id = w.organization_id
    FROM work_orders w WHERE h.work_order_id = w.id;
ALTER TABLE work_order_status_history ALTER COLUMN organization_id SET NOT NULL;
ALTER TABLE work_order_status_history ADD CONSTRAINT fk_history_organization
    FOREIGN KEY (organization_id) REFERENCES organizations(id);
CREATE INDEX idx_work_order_history_organization ON work_order_status_history(organization_id);