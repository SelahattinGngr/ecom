INSERT INTO permissions (name, description) VALUES
('site:read',   'Site ayarlarını ve asset slotlarını görüntüleme'),
('site:manage', 'Site ayarlarını güncelleme, asset yükleme ve slot yönetimi');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.name = 'developer' AND p.name IN ('site:read', 'site:manage');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.name = 'admin' AND p.name IN ('site:read', 'site:manage');
