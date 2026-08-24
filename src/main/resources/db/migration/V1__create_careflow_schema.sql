CREATE TABLE service_sites (
    id UUID PRIMARY KEY,
    site_code VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    customer_name VARCHAR(160) NOT NULL,
    address_line_1 VARCHAR(180) NOT NULL,
    city VARCHAR(80) NOT NULL,
    state VARCHAR(80) NOT NULL,
    postal_code VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE assets (
    id UUID PRIMARY KEY,
    asset_tag VARCHAR(40) NOT NULL UNIQUE,
    name VARCHAR(140) NOT NULL,
    category VARCHAR(80) NOT NULL,
    manufacturer VARCHAR(100),
    model VARCHAR(100),
    serial_number VARCHAR(100),
    status VARCHAR(30) NOT NULL,
    site_id UUID NOT NULL REFERENCES service_sites(id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_asset_status CHECK (status IN ('OPERATIONAL', 'DEGRADED', 'OUT_OF_SERVICE', 'RETIRED'))
);

CREATE TABLE technicians (
    id UUID PRIMARY KEY,
    employee_code VARCHAR(30) NOT NULL UNIQUE,
    full_name VARCHAR(120) NOT NULL,
    email VARCHAR(160) NOT NULL UNIQUE,
    primary_skill VARCHAR(100) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE work_orders (
    id UUID PRIMARY KEY,
    reference_number VARCHAR(40) NOT NULL UNIQUE,
    title VARCHAR(180) NOT NULL,
    description VARCHAR(4000) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL,
    site_id UUID NOT NULL REFERENCES service_sites(id),
    asset_id UUID REFERENCES assets(id),
    assigned_technician_id UUID REFERENCES technicians(id),
    target_resolution_at TIMESTAMPTZ NOT NULL,
    resolved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_work_order_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT chk_work_order_status CHECK (status IN ('NEW', 'ASSIGNED', 'IN_PROGRESS', 'ON_HOLD', 'RESOLVED', 'CANCELLED'))
);

CREATE INDEX idx_work_orders_status ON work_orders(status);
CREATE INDEX idx_work_orders_target_resolution ON work_orders(target_resolution_at);
CREATE INDEX idx_work_orders_site ON work_orders(site_id);
CREATE INDEX idx_work_orders_technician ON work_orders(assigned_technician_id);

CREATE TABLE work_order_status_history (
    id UUID PRIMARY KEY,
    work_order_id UUID NOT NULL REFERENCES work_orders(id) ON DELETE CASCADE,
    from_status VARCHAR(30),
    to_status VARCHAR(30) NOT NULL,
    note VARCHAR(500),
    changed_by VARCHAR(120) NOT NULL,
    changed_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_work_order_history_order ON work_order_status_history(work_order_id, changed_at);

