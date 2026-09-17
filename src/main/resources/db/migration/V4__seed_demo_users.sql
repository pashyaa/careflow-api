-- Demo login accounts, one per role. Local/dev only — rotate before any shared environment.
--   planner.pat@careflow.local     / Planner#2026        (ROLE_PLANNER)
--   tech.jamie@careflow.local      / Technician#2026      (ROLE_TECHNICIAN)
--   viewer.morgan@careflow.local   / Viewer#2026           (ROLE_CUSTOMER_VIEWER)
--   admin.riley@careflow.local     / Administrator#2026    (ROLE_ADMIN)

INSERT INTO users (id, email, password, enabled) VALUES
                                                     ('60000000-0000-0000-0000-000000000001', 'planner.pat@careflow.local', '$2b$12$iw6/LH.hriXaAJJje3RmJeLF9A4nP2BibEw0fuyBk8IwpRIX1skqO', TRUE),
                                                     ('60000000-0000-0000-0000-000000000002', 'tech.jamie@careflow.local', '$2b$12$y.nFKSGAeyYy0NGGDSULn.AtX7xBu/Lfutkw7pAXK5FkI/JjUdAEG', TRUE),
                                                     ('60000000-0000-0000-0000-000000000003', 'viewer.morgan@careflow.local', '$2b$12$ggbb/CzchNnMr7Jt.ZlH6OQAS90joz/5MtegqJEhQ2uuAQF3VWgiO', TRUE),
                                                     ('60000000-0000-0000-0000-000000000004', 'admin.riley@careflow.local', '$2b$12$RQT1MbYrI3vgA0ZG9hhhXuwh.O1cwbe1EDgettukpDKf.F38/wQJS', TRUE);

INSERT INTO user_roles (user_id, role_id)
SELECT '60000000-0000-0000-0000-000000000001'::uuid, id FROM roles WHERE name = 'ROLE_PLANNER'
UNION ALL
SELECT '60000000-0000-0000-0000-000000000002'::uuid, id FROM roles WHERE name = 'ROLE_TECHNICIAN'
UNION ALL
SELECT '60000000-0000-0000-0000-000000000003'::uuid, id FROM roles WHERE name = 'ROLE_CUSTOMER_VIEWER'
UNION ALL
SELECT '60000000-0000-0000-0000-000000000004'::uuid, id FROM roles WHERE name = 'ROLE_ADMIN';