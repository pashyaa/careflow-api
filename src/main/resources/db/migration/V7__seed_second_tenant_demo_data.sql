-- CF-102: a second tenant, deliberately reusing the SAME site code, asset tag,
-- asset name, technician employee code, and work order title as tenant 1's demo
-- data. This is the fixture the acceptance test drives: "create same-named
-- assets in two tenants and verify no leakage in filters, details, dashboard,
-- or history."

INSERT INTO organizations (id, slug, name, created_at) VALUES
    ('70000000-0000-0000-0000-000000000002', 'solstice-industrial', 'Solstice Industrial Services', CURRENT_TIMESTAMP);

-- One login per role, isolated to tenant 2.
--   planner.dana@solstice.local     / Planner#2026        (ROLE_PLANNER)
--   admin.kai@solstice.local        / Administrator#2026  (ROLE_ADMIN)
INSERT INTO users (id, email, password, enabled, display_name, organization_id) VALUES
                                                                                    ('60000000-0000-0000-0000-000000000005', 'planner.dana@solstice.local',
                                                                                     '$2b$12$iw6/LH.hriXaAJJje3RmJeLF9A4nP2BibEw0fuyBk8IwpRIX1skqO', TRUE, 'Dana Whitfield',
                                                                                     '70000000-0000-0000-0000-000000000002'),
                                                                                    ('60000000-0000-0000-0000-000000000006', 'admin.kai@solstice.local',
                                                                                     '$2b$12$RQT1MbYrI3vgA0ZG9hhhXuwh.O1cwbe1EDgettukpDKf.F38/wQJS', TRUE, 'Kai Osei',
                                                                                     '70000000-0000-0000-0000-000000000002');

INSERT INTO user_roles (user_id, role_id)
SELECT '60000000-0000-0000-0000-000000000005'::uuid, id FROM roles WHERE name = 'ROLE_PLANNER'
UNION ALL
SELECT '60000000-0000-0000-0000-000000000006'::uuid, id FROM roles WHERE name = 'ROLE_ADMIN';

-- Same site_code ('BLR-DC-01') and name as tenant 1 — allowed now that the
-- uniqueness constraint is scoped per tenant.
INSERT INTO service_sites (id, organization_id, site_code, name, customer_name, address_line_1, city, state, postal_code, active, created_at, updated_at) VALUES
    ('10000000-0000-0000-0000-000000000101', '70000000-0000-0000-0000-000000000002',
     'BLR-DC-01', 'Bengaluru Distribution Centre', 'Solstice Cold Chain Pvt Ltd',
     '9 Ring Road Logistics Hub', 'Bengaluru', 'Karnataka', '560045', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Same asset_tag ('HVAC-BLR-014') and name as tenant 1's asset.
INSERT INTO assets (id, organization_id, asset_tag, name, category, manufacturer, model, serial_number, status, site_id, created_at, updated_at) VALUES
    ('20000000-0000-0000-0000-000000000101', '70000000-0000-0000-0000-000000000002',
     'HVAC-BLR-014', 'Cold Storage HVAC Unit 14', 'HVAC', 'Daikin', 'VRV-IV-X', 'DK-VRV-90142',
     'OPERATIONAL', '10000000-0000-0000-0000-000000000101', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Same employee_code ('TECH-1042') as tenant 1's technician.
INSERT INTO technicians (id, organization_id, employee_code, full_name, email, primary_skill, active, created_at, updated_at) VALUES
    ('30000000-0000-0000-0000-000000000101', '70000000-0000-0000-0000-000000000002',
     'TECH-1042', 'Farhan Sheikh', 'farhan.sheikh@solstice.demo', 'HVAC and refrigeration',
     TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Same title/wording as tenant 1's cooling work order, different reference_number
-- (still globally distinct, but only required to be unique within the tenant).
INSERT INTO work_orders (id, organization_id, reference_number, title, description, priority, status, site_id, asset_id, assigned_technician_id, target_resolution_at, resolved_at, created_at, updated_at, version) VALUES
    ('40000000-0000-0000-0000-000000000101', '70000000-0000-0000-0000-000000000002',
     'WO-20260821-S1E4T091', 'Cold room temperature drift above threshold',
     'Second tenant, same symptom description as tenant 1 by design, to exercise isolation checks.',
     'CRITICAL', 'ASSIGNED', '10000000-0000-0000-0000-000000000101', '20000000-0000-0000-0000-000000000101',
     '30000000-0000-0000-0000-000000000101', CURRENT_TIMESTAMP + INTERVAL '8 hours', NULL,
     CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP - INTERVAL '1 hour', 1);

INSERT INTO work_order_status_history (id, organization_id, work_order_id, from_status, to_status, note, changed_by_user_id, changed_by_display_name, changed_at) VALUES
                                                                                                                                                                      ('50000000-0000-0000-0000-000000000101', '70000000-0000-0000-0000-000000000002',
                                                                                                                                                                       '40000000-0000-0000-0000-000000000101', NULL, 'NEW', 'Alert converted into a work order',
                                                                                                                                                                       '60000000-0000-0000-0000-000000000005', 'Dana Whitfield', CURRENT_TIMESTAMP - INTERVAL '1 day'),
                                                                                                                                                                      ('50000000-0000-0000-0000-000000000102', '70000000-0000-0000-0000-000000000002',
                                                                                                                                                                       '40000000-0000-0000-0000-000000000101', 'NEW', 'ASSIGNED', 'Assigned to refrigeration specialist',
                                                                                                                                                                       '60000000-0000-0000-0000-000000000005', 'Dana Whitfield', CURRENT_TIMESTAMP - INTERVAL '1 hour');