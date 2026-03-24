INSERT INTO permissions (name, description) VALUES
('refund:read',   'İade kayıtlarını ve detaylarını görüntüleme'),
('refund:manage', 'İade durumunu güncelleme ve yönetme');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.name = 'developer' AND p.name IN ('refund:read', 'refund:manage');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.name = 'admin' AND p.name IN ('refund:read', 'refund:manage');
