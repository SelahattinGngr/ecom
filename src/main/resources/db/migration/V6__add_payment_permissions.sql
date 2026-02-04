INSERT INTO permissions (name, description) VALUES
('payment:read',   'Ödemeleri ve detaylarını görüntüleme'),
('payment:manage', 'Ödeme iadesi, iptali veya manuel tahsilat (Capture/Void)');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p 
WHERE r.name = 'developer' AND p.name IN ('payment:read', 'payment:manage');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p 
WHERE r.name = 'admin' AND p.name IN ('payment:read', 'payment:manage');
