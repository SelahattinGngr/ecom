INSERT INTO permissions (name, description) VALUES
    ('analytics:read', 'Analitik verilerini görüntüleme')
ON CONFLICT (name) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name IN ('admin', 'developer')
  AND p.name = 'analytics:read'
ON CONFLICT DO NOTHING;
