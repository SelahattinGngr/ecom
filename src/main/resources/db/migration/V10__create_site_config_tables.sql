-- Varsayılan ayarlar (varsa güncelle, yoksa ekle)
INSERT INTO site_settings (setting_key, value_json) VALUES
('site_name',      '"My Shop"'),
('support_email',  '"support@myshop.com"')
ON CONFLICT (setting_key) DO NOTHING;

-- Varsayılan asset slotları (varsa atla)
INSERT INTO site_asset_slots (slot_key) VALUES ('logo'), ('favicon')
ON CONFLICT (slot_key) DO NOTHING;
