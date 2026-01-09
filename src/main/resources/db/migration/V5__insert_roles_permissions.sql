INSERT INTO roles (name, description, is_system)
VALUES ('developer', 'Sistem Yöneticisi / Developer', TRUE),
       ('admin', 'Admin role', TRUE),
       ('customer', 'Customer role', TRUE);
       
INSERT INTO permissions (name, description) VALUES
('system:manage',    'Sistem ayarlarına erişim ve log okuma'),
('dashboard:view',   'Admin paneli dashboard görüntüleme'),
('product:read',     'Ürünleri listeleme ve detay görme'),
('product:create',   'Yeni ürün oluşturma'),
('product:update',   'Ürün güncelleme (Fiyat/Stok)'),
('product:delete',   'Ürün silme'),
('product:view',     'Ürünleri görüntüleme'),
('category:manage',  'Kategori ekleme/silme/düzenleme'),
('order:read',       'Siparişleri görüntüleme'),
('order:update',     'Sipariş durumu güncelleme (Kargolandı vs.)'),
('order:cancel',     'Siparişi iptal etme'),
('user:read',        'Kullanıcıları listeleme'),
('user:manage',      'Kullanıcı banlama veya rol değiştirme');

-- Yetkileri Rollere Dağıt (OTOMATİK)

-- DEVELOPER: Hepsini alır
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p WHERE r.name = 'developer';

-- ADMIN: system:manage HARİÇ hepsini alır
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p WHERE r.name = 'admin';

-- CUSTOMER: Sadece product:view iznini alır
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.name = 'product:view' WHERE r.name = 'customer';