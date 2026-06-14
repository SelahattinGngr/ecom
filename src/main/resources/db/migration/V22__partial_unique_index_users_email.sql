-- B-06: Silinmiş kullanıcının e-postasıyla yeni hesap açılabilmesi için
-- global UNIQUE kısıtı kaldırılıp yalnızca aktif kayıtları kapsayan
-- partial unique index ekleniyor.
--
-- Önceki davranış: UNIQUE(email) — tüm satırları kapsıyor, silinmiş maile
--                  INSERT yapmaya çalışınca DataIntegrityViolationException (500).
-- Yeni davranış:   UNIQUE WHERE deleted_at IS NULL — yalnızca aktif kullanıcılar
--                  arasında benzersizlik zorunlu; silinmiş mail "serbest" kalıyor.

ALTER TABLE users DROP CONSTRAINT IF EXISTS users_email_key;

CREATE UNIQUE INDEX users_email_unique_active
    ON users (email)
    WHERE deleted_at IS NULL;
